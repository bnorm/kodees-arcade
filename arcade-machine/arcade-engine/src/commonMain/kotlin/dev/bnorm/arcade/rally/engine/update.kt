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
import kotlin.math.sqrt

internal const val impactDist = 27.2
internal const val impactDistSq = impactDist * impactDist
internal const val borderImpactDist = impactDist / 2.0

internal val draftAngle = Angle.QUARTER_CIRCLE / 4.0 // 22.5 deg
internal const val draftDist = 10 * 6 + impactDist // 10 meters + impact

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
            newSpeed = 0.0
        }

        state.heading = newHeading
        state.speed = newSpeed
    }

    // TODO optimize driver collisions
    //  better representation for the cars
    //    rotated ovals?
    //    convex polygons?
    //  should impacts effect speed?
    //    this might make the physics a little more complicated than it needs to be...

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
                    driver1.speed = 0.0
                }
                if (move(driver2, impulse, angle + Angle.HALF_CIRCLE)) {
                    driver2.speed = 0.0
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

    gameState.finished = drivers.all { it.finished }
}

private fun DriverState.computeDraftBoost(other: DriverState): Double {
    if (this === other) return 0.0
    if (other.finished) return 0.0

    val distSq = distanceSq(other)
    if (distSq < draftDist * draftDist) {
        // TODO should this bearing be from the perspective of the other car?
        val absBearingTo = abs((angleTo(other) - heading).toRelative())
        if (absBearingTo < draftAngle && abs(heading - other.heading) < draftAngle) {
            return MAX_SPEED_BOOST *
                (1.5 - sqrt(distSq) / (draftDist - impactDist)).coerceAtMost(1.0) *
                (other.speed / MAX_SPEED).coerceAtMost(1.0)
        }
    }

    return 0.0
}
