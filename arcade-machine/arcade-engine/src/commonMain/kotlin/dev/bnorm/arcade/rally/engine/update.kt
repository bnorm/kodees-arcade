package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.abs
import dev.bnorm.arcade.geometry.atan2
import dev.bnorm.arcade.geometry.cos
import dev.bnorm.arcade.geometry.nearest
import dev.bnorm.arcade.geometry.sin
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.MAX_SPEED
import dev.bnorm.arcade.rally.MAX_SPEED_BOOST
import dev.bnorm.arcade.rally.simulateHeading
import dev.bnorm.arcade.rally.simulateSpeed
import kotlin.math.sign
import kotlin.math.sqrt

internal const val impactDist = 27.2
internal const val impactDistSq = impactDist * impactDist
internal const val borderImpactDist = impactDist / 2.0

internal val draftAngle = Angle.QUARTER_CIRCLE / 2.0 // 45 degrees
internal const val draftDist = 20.0 * 6.0 // 20 meters

fun update(gameState: GameState) {
    gameState.time++

    /** @return if impacted with a wall. */
    fun move(state: DriverState, speed: Double, heading: Angle): Boolean {
        val newX = state.x + speed * cos(heading)
        val newY = state.y + speed * sin(heading)
        state.x = newX
        state.y = newY

        if (newX !in gameState.widthRange || newY !in gameState.heightRange) {
            state.x = newX.coerceIn(gameState.widthRange)
            state.y = newY.coerceIn(gameState.heightRange)
            return true
        }

        return false
    }

    val drivers = gameState.driverStates
    for (state in drivers) {
        // Skip updating drivers which are finished.
        if (state.finished) continue

        val steering = state.driver.steering
        val throttle = state.driver.throttle

        val oldHeading = state.heading
        val oldSpeed = state.speed

        // TODO consider traction of track
        //  - is there an official curb for the track so a little run-off or corner cutting is okay?
        //  - does the "weight" of the car on each tire, caused by breaking and/or turning,
        //    impact the traction contribution of each tire?

        val boost = drivers.maxOf { state.computeDraftBoost(it) }
        val newHeading = simulateHeading(oldHeading, oldSpeed, steering, traction = 1.0)
        var newSpeed = simulateSpeed(oldSpeed, boost, throttle)
        if (move(state, newSpeed, newHeading)) {
            newSpeed = 0.0 // Hit a wall!
        }

        state.heading = newHeading
        state.speed = newSpeed
    }

    // TODO optimize driver collisions
    //  better representation for the cars
    //    rotated ovals?
    //    convex polygons?

    // Only do a single pass...
    // TODO is a little bit of clipping okay?

    for ((i, driver1) in drivers.withIndex()) {
        // Skip updating drivers which are finished.
        if (driver1.finished) continue

        for (j in (i + 1)..<drivers.size) {
            val driver2 = drivers[j]
            // Skip updating drivers which are finished.
            if (driver2.finished) continue

            val dx = driver1.x - driver2.x
            val dy = driver1.y - driver2.y
            val distSq = dx * dx + dy * dy
            if (distSq < impactDistSq) {
                val delta = sqrt(impactDistSq) - sqrt(distSq)
                val angle = atan2(dy, dx)
                val impulse = (delta / 2).coerceAtLeast(0.1)
                if (move(driver1, impulse, angle)) {
                    driver1.speed = 0.0 // Hit a wall!
                } else {
                    // Adjust driver speed based on impact.
                    val speedAdj = impulse * cos(angle - driver1.heading)
                    // Bump drafting should *not* push the leader faster!
                    if (sign(driver1.speed) * speedAdj < 0) {
                        driver1.speed += speedAdj
                    }
                }
                if (move(driver2, impulse, angle + Angle.HALF_CIRCLE)) {
                    driver2.speed = 0.0 // Hit a wall!
                } else {
                    // Adjust driver speed based on impact.
                    val speedAdj = impulse * cos(angle + Angle.HALF_CIRCLE - driver2.heading)
                    // Bump drafting should *not* push the leader faster!
                    if (sign(driver2.speed) * speedAdj < 0) {
                        driver2.speed += speedAdj
                    }
                }
            }
        }
    }

    // Update driver checkpoints.
    for (driverState in drivers) {
        val checkpoint = gameState.track.checkpoints[driverState.checkpoint]
        val radius = (MAX_SPEED + 1)

        // Calculate distance to the checkpoint by
        // finding the nearest point on the checkpoint segment.
        val location = Point(driverState.x, driverState.y)
        val distSq = location.distanceSquaredTo(location.nearest(checkpoint))

        if (distSq < radius * radius) {
            driverState.checkpoint += 1

            // Proceed to the next lap.
            if (driverState.checkpoint >= gameState.track.checkpoints.size) {
                driverState.lap += 1
                driverState.checkpoint = 0
            }

            // Record lap time *after crossing* the 0th checkpoint.
            if (driverState.lap != 0 && driverState.checkpoint == 1) {
                driverState.lapTimes.add(gameState.time)
            }

            if (driverState.lap >= gameState.laps && driverState.checkpoint > 0) {
                driverState.finished = true
            }
        }
    }

    // TODO driver should know its current race position
    //  - update it here so driver can be notified as part of onTurn()
    //  - or do we instead force the driver to track this based on all the other drivers?

    gameState.finished = drivers.all { it.finished }
}

// TODO move this to Physics.kt
private fun DriverState.computeDraftBoost(other: DriverState): Double {
    if (this === other) return 0.0
    if (other.finished) return 0.0

    val distSq = distanceSq(other) // Center to center distance.
    if (distSq < (draftDist + impactDist) * (draftDist + impactDist)) {
        val absReverseBearing = abs((other.angleTo(this) + Angle.HALF_CIRCLE - other.heading).toRelative())
        val absHeadingDiff = abs((heading - other.heading).toRelative())
        if (absReverseBearing < draftAngle && absHeadingDiff < Angle.QUARTER_CIRCLE) {
            // Each of these multiples indicates something which can impact how much of the speed boost
            // can be applied from the lead car to the following car.

            // When within this percent of the target value, the multiple should always be 1.0.
            val plateau = 0.2

            // How far away is the following car from the lead car?
            val dist = calculateInverseMultiple(
                actual = (sqrt(distSq) - impactDist - draftDist * plateau).coerceAtLeast(0.0),
                opposite = draftDist * (1.0 - plateau),
            )

            // How fast is the lead car actually going?
            val speed = calculateMultiple(
                actual = other.speed,
                target = MAX_SPEED * (1.0 - plateau),
            )

            // At what angle behind the lead car, relative to lead car's heading, is the following car?
            val bearing = calculateInverseMultiple(
                actual = (absReverseBearing - draftAngle * plateau).coerceAtLeast(Angle.ZERO),
                opposite = draftAngle * (1.0 - plateau),
            )

            // At what angle is the following car's heading relative to the lead car's heading?
            val heading = calculateInverseMultiple(
                actual = (absHeadingDiff - Angle.QUARTER_CIRCLE * plateau).coerceAtLeast(Angle.ZERO),
                opposite = Angle.QUARTER_CIRCLE * (1.0 - plateau),
            )

            return MAX_SPEED_BOOST * (dist * speed * bearing * heading)
        }
    }

    return 0.0
}


/**
 * Returns a value in the range in `[0..1]`, depending on where [actual] is in the range `[0..max]` respectively.
 */
private fun calculateMultiple(actual: Double, target: Double): Double {
    return (actual / target).coerceIn(0.0, 1.0)
}

/**
 * Returns a value in the range in `[0..1]`, depending on where [actual] is in the range `[max..0]` respectively.
 */
private fun calculateInverseMultiple(actual: Double, opposite: Double): Double {
    return ((opposite - actual) / opposite).coerceIn(0.0, 1.0)
}

/**
 * Returns a value in the range in `[0..1]`, depending on where [actual] is in the range `[max..0]` respectively.
 */
private fun calculateInverseMultiple(actual: Angle, opposite: Angle): Double {
    return ((opposite - actual) / opposite).coerceIn(0.0, 1.0)
}
