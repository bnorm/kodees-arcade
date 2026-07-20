package dev.bnorm.arcade.driver.sample

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Controls
import dev.bnorm.arcade.driver.Driver
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.driver.canvas.Canvas
import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Line
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.abs
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.cos
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.plus
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.times
import dev.bnorm.arcade.geometry.toPoint
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.DECELERATION
import dev.bnorm.arcade.rally.MAX_SPEED
import dev.bnorm.arcade.rally.getTurningRadius
import kotlin.math.ceil

/**
 * Our driver! Kodee!
 *
 * This object implements the logic used to control our car during a race.
 */
object Kodee : Driver() {
    private lateinit var track: Track
    private lateinit var targets: List<Point>

    override fun onRace(race: Race) {
        this.track = race.track

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

    private lateinit var car: Car
    override fun move(car: Car, controls: Controls) {
        this.car = car

        val velocity = car.velocity
        val current = targets[car.nextCheckpoint]

        // Compute required steering for turning to target point.
        val bearing = car.bearingTo(current)
        val steering = getSteeringForTurn(bearing, velocity.magnitude)
        controls.steering = steering

        // Compute required throttle to make that turn.
        if (abs(bearing) < Angle.ofDegrees(0.1)) {
            // Pointed "directly" at the target! Full speed ahead!
            controls.throttle = 1.0

            // Let's check how we're lined up for the *next* checkpoint.
            val next = targets[(car.nextCheckpoint + 1) % targets.size]
            val nextAngle = current.angleTo(next)
            val nextAbsBearing = abs((nextAngle - car.velocity.angle).toRelative())
            if (nextAbsBearing < Angle.QUARTER_CIRCLE) {
                // This is the speed we need to be going when we reach the checkpoint so we can make the next turn.
                val nextDistance = current.distanceTo(next)
                val targetRadius = nextDistance / (2.0 * cos(Angle.QUARTER_CIRCLE - nextAbsBearing))
                val targetSpeed = getMaxSpeed(targetRadius)

                // How much time will it take to decelerate?
                val time = ceil((velocity.magnitude - targetSpeed) / DECELERATION)
                if (time > 0) {
                    val decelerationDist = (time * (time + 1.0) / 2.0) * DECELERATION + time * targetSpeed
                    // Are we close enough that we need to start decelerating?
                    if (car.location.distanceSquaredTo(current) <= decelerationDist * decelerationDist) {
                        // Slam on the breaks!
                        controls.throttle = 0.0
                    }
                }
            }
        } else {
            // Calculate our car's pivot point.
            val radius = getTurningRadius(velocity.magnitude)
            val normalHeading = velocity.angle + sign(bearing) * Angle.QUARTER_CIRCLE
            val pivot = car.location.plus(normalHeading, radius)

            // Compute speed based on distance of target from pivot.
            // When the turn is severe, it's better to slow down a little
            // TODO there's probably a better way to do this than a turn ratio...
            //  - maybe a distance penalty instead?
            //  - what we really want is to optimize *time* it takes to get to the next checkpoint!
            //  - so is it faster to slow down to make a tighter turn and then speed up again,
            //    also covering a smaller amount of total distance?
            val dist = pivot.distanceTo(current)
            val turnRatio = 1.0 - abs(bearing) / Angle.HALF_CIRCLE
            val throttle = getMaxThrottle(dist) * turnRatio * turnRatio

            // No point in going slower than the maximum speed for the minimum turning radius.
            controls.throttle = throttle.coerceAtLeast(TURNING_RADIUS_SPEED / MAX_SPEED)
        }
    }

    override fun onDraw(canvas: Canvas) {
        repeat(targets.size) {
            val start = targets[it]
            val end = targets[(it + 1) % targets.size]
            canvas.drawSegment(
                color = Color.Blue,
                segment = Segment(start, end),
                stroke = Stroke(width = 2f)
            )
        }

        val radius = getTurningRadius(car.velocity.magnitude)
        val normalVector = Vector(
            angle = car.velocity.angle + Angle.QUARTER_CIRCLE,
            magnitude = radius,
        ).toPoint()

        canvas.drawCircle(
            color = Color.Red,
            circle = Circle(car.location + normalVector, radius = radius),
            style = Stroke(width = 2f),
        )
        canvas.drawCircle(
            color = Color.Red,
            circle = Circle(car.location - normalVector, radius = radius),
            style = Stroke(width = 2f),
        )
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
