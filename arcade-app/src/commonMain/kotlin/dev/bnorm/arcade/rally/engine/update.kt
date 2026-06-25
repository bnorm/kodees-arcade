package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.geometry.atan2
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.cos
import dev.bnorm.arcade.geometry.length
import dev.bnorm.arcade.rally.simulateHeading
import dev.bnorm.arcade.rally.simulateSpeed
import dev.bnorm.arcade.geometry.sin
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0
//val impactDist = carHeight / 2.0
val impactDist = (68.0 * 0.4f)
val impactDistSq = impactDist * impactDist

fun update(gameState: RallyGameState, controls: Map<String, RacerControlState>, track: Track): RallyGameState {
    val racers = gameState.racers.mapValues { RallyCarState.Mutable(it.value) }
    for ((name, racerState) in racers) {
        // Skip updating racers which are finished.
        if (racerState.lap >= track.laps) continue

        val controls = controls.getValue(name)
        val steering = controls.steering
        val throttle = controls.throttle

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

    val racerList = racers.values.toList()
    for ((i, racer1) in racerList.withIndex()) {
        // Skip updating racers which are finished.
        if (racer1.lap >= track.laps) continue

        for (j in (i + 1)..<racers.size) {
            val racer2 = racerList[j]
            // Skip updating racers which are finished.
            if (racer2.lap >= track.laps) continue

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

    return RallyGameState(
        trackWidth = gameState.trackWidth,
        trackHeight = gameState.trackHeight,
        finished = racers.all { it.value.lap >= track.laps },
        time = gameState.time + 1,
        racers = racers.mapValues { it.value.build() }
    )
}

/** @return if impacted with a wall. */
private fun updatePosition(
    state: RallyCarState.Mutable,
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