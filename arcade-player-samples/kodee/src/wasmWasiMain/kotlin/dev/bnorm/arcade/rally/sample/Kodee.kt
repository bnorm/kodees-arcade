package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Line
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.abs
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.*

/**
 * Our racer! Kodee!
 *
 * This object implements the logic used to control our car during a race.
 */
object Kodee : Racer() {
    private lateinit var track: Track
    private lateinit var targets: List<Point>

    override fun onRace(track: Track) {
        this.track = track

        val count = track.checkpoints.size

        val centers = MutableList(count) { track.checkpoints[it].center }

        val entrances = MutableList(count) { Point.ZERO }
        val targets = MutableList(count) { Point.ZERO }
        val exits = MutableList(count) { Point.ZERO }

        repeat(count) {
            val entranceIndex = (count + it - 2) % count
            val startIndex = (count + it - 1) % count
            val endIndex = (it + 1) % count
            val exitIndex = (it + 2) % count

            val racingLine = Line(centers[startIndex], centers[endIndex])

            val entrance = racingLine.findTarget(track.checkpoints[entranceIndex])
            val target = racingLine.findTarget(track.checkpoints[it])
            val exit = racingLine.findTarget(track.checkpoints[exitIndex])

            entrances[entranceIndex] = entrance
            targets[it] = target
            exits[exitIndex] = exit
        }

        fun avg(vararg points: Point): Point {
            return Point(
                x = points.sumOf { it.x } / points.size,
                y = points.sumOf { it.y } / points.size,
            )
        }

        this.targets = List(count) {
            avg(entrances[it], targets[it], targets[it], exits[it])
        }
    }

    override fun move(car: Car, controls: Controls) {
        val velocity = car.velocity
        val target = targets[car.nextCheckpoint]

        val bearing = car.bearingTo(target)
        val steering = steeringToBearing(bearing, velocity)
        controls.steering = steering

        fun turnGuess(throttle: Double): Angle = getTurn(throttle * MAX_SPEED, steering = 1.0, traction = 1.0)

        // TODO need to consider distance as well!!!
        val requiredTurn = abs(bearing)
        controls.throttle = when {
            requiredTurn < turnGuess(1.0) -> 1.0
            requiredTurn < turnGuess(0.9) -> 0.9
            requiredTurn < turnGuess(0.8) -> 0.8
            requiredTurn < turnGuess(0.7) -> 0.7
            requiredTurn < turnGuess(0.6) -> 0.6
            requiredTurn < turnGuess(0.5) -> 0.5
            requiredTurn < turnGuess(0.4) -> 0.4
            requiredTurn < turnGuess(0.3) -> 0.3
            requiredTurn < turnGuess(0.2) -> 0.2
            else -> 0.1
        }
    }
}

private fun Car.bearingTo(target: Point): Angle {
    return (location.angleTo(target) - velocity.angle).toRelative()
}

private fun Line.findTarget(checkpoint: Segment): Point {
    val intersection = (this intersect checkpoint.toLine())!!
    return intersection.coerceIn(checkpoint)
}

private fun Segment.toLine(): Line {
    return Line(start, end)
}

private fun Point.coerceIn(segment: Segment): Point {
    val (startX, startY) = segment.start
    val (endX, endY) = segment.end
    return Point(
        x = x.coerceIn(minOf(startX, endX), maxOf(startX, endX)),
        y = y.coerceIn(minOf(startY, endY), maxOf(startY, endY)),
    )
}
