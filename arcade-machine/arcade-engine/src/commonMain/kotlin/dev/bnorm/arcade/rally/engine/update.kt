package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.atan2
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.cos
import dev.bnorm.arcade.geometry.length
import dev.bnorm.arcade.geometry.sin
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.simulateHeading
import dev.bnorm.arcade.rally.simulateSpeed
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0
//val impactDist = carHeight / 2.0
val impactDist = (68.0 * 0.4f)
val impactDistSq = impactDist * impactDist

fun update(gameState: RallyGameState, track: Track) {
    gameState.time++

    val drivers = gameState.drivers
    for (driverState in drivers) {
        // Skip updating drivers which are finished.
        if (driverState.lap >= track.laps) {
            if (driverState.finished == null) {
                driverState.finished = gameState.time
            }
            continue
        }

        val controls = driverState.controls
        val steering = controls.steering
        val throttle = controls.throttle

        val oldHeading = driverState.heading
        val oldSpeed = driverState.speed

        // TODO consider traction of track
        val newHeading = simulateHeading(oldHeading, oldSpeed, steering, traction = 1.0)
        var newSpeed = simulateSpeed(oldSpeed, throttle)
        if (updatePosition(driverState, newSpeed, newHeading, gameState)) {
            newSpeed = 0.0
        }

        driverState.heading = newHeading
        driverState.speed = newSpeed

        // Update target checkpoint.
        val checkpoint = track.checkpoints[driverState.checkpoint]
        val target = checkpoint.center
        val radius = checkpoint.length / 2

        val dx = target.x - driverState.x
        val dy = (target.y) - driverState.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < radius) {
            driverState.checkpoint += 1
            if (driverState.checkpoint >= track.checkpoints.size) {
                driverState.lap += 1
                driverState.checkpoint = 0
            }
        }
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
        if (driver1.lap >= track.laps) continue

        for (j in (i + 1)..<drivers.size) {
            val driver2 = drivers[j]
            // Skip updating drivers which are finished.
            if (driver2.lap >= track.laps) continue

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

    gameState.finished = drivers.all { it.finished != null }
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
