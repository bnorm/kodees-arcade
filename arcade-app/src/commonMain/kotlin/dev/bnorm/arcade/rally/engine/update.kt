package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.rally.Angle
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.atan2
import dev.bnorm.arcade.rally.center
import dev.bnorm.arcade.rally.cos
import dev.bnorm.arcade.rally.length
import dev.bnorm.arcade.rally.simulateHeading
import dev.bnorm.arcade.rally.simulateSpeed
import dev.bnorm.arcade.rally.sin
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0
//val impactDist = carHeight / 2.0
val impactDist = (68.0 * 0.4f)
val impactDistSq = impactDist * impactDist

fun update(gameState: RallyGameState, track: Track) {
    gameState.time++

    val racers = gameState.racers.values.toList()
    for (racerState in racers) {
        val steering = racerState.steering
        val throttle = racerState.throttle

        val oldHeading = racerState.heading
        val oldSpeed = racerState.speed

        // TODO consider traction of track
        val newHeading = simulateHeading(oldHeading, oldSpeed, steering, traction = 1.0)
        var newSpeed = simulateSpeed(oldSpeed, throttle)
        if (updatePosition(racerState, newSpeed, newHeading, gameState)) {
            newSpeed = 0.0
        }

        racerState.heading = newHeading
        racerState.speed = newSpeed

        // Update target checkpoint.
        val checkpoint = track.checkpoints[racerState.checkpoint]
        val target = checkpoint.center
        val radius = checkpoint.length / 2

        val dx = target.x - racerState.x
        val dy = (target.y) - racerState.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < radius) {
            racerState.checkpoint += 1
            if (racerState.checkpoint >= track.checkpoints.size) {
                racerState.lap += 1
                racerState.checkpoint = 0
            }
        }
    }

    // TODO optimize racer collisions
    //  better representation for the cars
    //    rotated ovals?
    //    convex polygons?
    //  should impacts effect speed?
    //    this might make the physics a little more complicated than it needs to be...

    // Only do a single pass...
    // TODO is a little bit of clipping okay?
    for ((i, racer1) in racers.withIndex()) {
        for (j in (i + 1)..<racers.size) {
            val racer2 = racers[j]

            val dx = racer1.x - racer2.x
            val dy = racer1.y - racer2.y
            val distSq = dx * dx + dy * dy
            if (distSq < impactDistSq) {
                val delta = sqrt(impactDistSq) - sqrt(distSq)
                val angle = atan2(dy, dx)
                val impulse = (delta / 2).coerceAtLeast(0.1)
                if (updatePosition(racer1, impulse, angle, gameState)) {
                    racer1.speed = 0.0
                }
                if (updatePosition(racer2, impulse, angle + Angle.HALF_CIRCLE, gameState)) {
                    racer2.speed = 0.0
                }
            }
        }
    }
}

/** @return if impacted with a wall. */
private fun updatePosition(
    racerState: RallyRacerState,
    magnitude: Double,
    heading: Angle,
    gameState: RallyGameState
): Boolean {
    val newX = racerState.x + magnitude * cos(heading)
    val newY = racerState.y + magnitude * sin(heading)
    racerState.x = newX
    racerState.y = newY

    if (
        newX !in impactDist..gameState.trackWidth - impactDist ||
        newY !in impactDist..gameState.trackHeight - impactDist
    ) {
        racerState.x = newX.coerceIn(impactDist, gameState.trackWidth - impactDist)
        racerState.y = newY.coerceIn(impactDist, gameState.trackHeight - impactDist)
        return true
    }

    return false
}