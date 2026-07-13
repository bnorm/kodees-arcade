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
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.rally.simulateHeading
import dev.bnorm.arcade.rally.simulateSpeed
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0

//val impactDist = carHeight / 2.0
private const val impactDist = (68.0 * 0.4f)
private const val impactDistSq = impactDist * impactDist

private val draftAngle = Angle.QUARTER_CIRCLE / 4.0 // 22.5 deg
private const val draftDist = 10 * 6 + impactDist // 10 meters + impact

fun update(gameState: RallyGameState, track: Track) {
    gameState.time++

    // TODO consider drafting behind another car
    //  - find cars which are located "behind" another car based on heading
    //  - give a max speed boost (and acceleration?) to those cars
    //    based on the leading car's speed
    //     - can't weave behind a car that is going slower to gain a speed boost
    //  - a car receives the largest of all available speed boosts
    //  - speed boost should slowly decay, slow enough to allow following car to overtake
    //     - boosted speed should increase understeer!

    val drivers = gameState.driverStates
    for (state in drivers) {
        // Skip updating drivers which are finished.
        if (state.lap >= gameState.laps && state.checkpoint > 0) {
            if (state.finished == null) {
                state.finished = gameState.time
            }
            continue
        }

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
        if (updatePosition(state, newSpeed, newHeading, gameState)) {
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
        if (driver1.finished != null) continue

        for (j in (i + 1)..<drivers.size) {
            val driver2 = drivers[j]
            // Skip updating drivers which are finished.
            if (driver2.finished != null) continue

            val dx = driver1.x - driver2.x
            val dy = driver1.y - driver2.y
            val distSq = dx * dx + dy * dy
            if (distSq < impactDistSq) {
                val delta = sqrt(impactDistSq) - sqrt(distSq)
                val angle = atan2(dy, dx)
                val impulse = (delta / 2).coerceAtLeast(0.1)
                if (updatePosition(driver1, impulse, angle, gameState)) {
                    driver1.speed = 0.0
                }
                if (updatePosition(driver2, impulse, angle + Angle.HALF_CIRCLE, gameState)) {
                    driver2.speed = 0.0
                }
            }
        }
    }

    // Update driver checkpoints.
    for (driverState in drivers) {
        val checkpoint = track.checkpoints[driverState.checkpoint]
        val radius = (MAX_SPEED + 1)

        // Calculate distance to the checkpoint by
        // finding the nearest point on the checkpoint segment.
        val location = Point(driverState.x, driverState.y)
        val distSq = location.distanceSquaredTo(location.nearest(checkpoint))

        if (distSq < radius * radius) {
            driverState.checkpoint += 1
            if (driverState.checkpoint >= track.checkpoints.size) {
                driverState.lap += 1
                driverState.checkpoint = 0
            }
        }
    }

    gameState.finished = drivers.all { it.finished != null }
}

private fun RallyCarState.computeDraftBoost(other: RallyCarState): Double {
    if (this === other) return 0.0
    if (other.finished != null) return 0.0

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

/** @return if impacted with a wall. */
private fun updatePosition(
    state: RallyCarState,
    magnitude: Double,
    heading: Angle,
    gameState: RallyGameState
): Boolean {
    val newX = state.x + magnitude * cos(heading)
    val newY = state.y + magnitude * sin(heading)
    state.x = newX
    state.y = newY

    if (
        newX !in impactDist..gameState.trackWidth - impactDist ||
        newY !in impactDist..gameState.trackHeight - impactDist
    ) {
        state.x = newX.coerceIn(impactDist, gameState.trackWidth - impactDist)
        state.y = newY.coerceIn(impactDist, gameState.trackHeight - impactDist)
        return true
    }

    return false
}
