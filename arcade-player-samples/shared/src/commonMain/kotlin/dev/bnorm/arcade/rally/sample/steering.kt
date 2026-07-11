package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.toRelative
import kotlin.math.abs

fun steeringToHeading(
    heading: Angle,
    velocity: Vector
): Double {
    return steeringToBearing(
        bearing = (heading - velocity.angle).toRelative(),
        velocity = velocity,
    )
}

fun steeringToBearing(
    bearing: Angle,
    velocity: Vector
): Double {
    val sign = sign(bearing)
    if (sign == 0.0) return 0.0

    val maxSteering = getMaxSteering(velocity.magnitude)
    val turn = getTurn(velocity.magnitude, steering = maxSteering, traction = 1.0)
    return sign * abs(bearing / turn).coerceAtMost(1.0)
}

fun getMaxSteering(speed: Double, traction: Double = 1.0): Double {
    if (speed == 0.0) return MAX_STEER
    val optimalRadius = speed * speed / (CORNERING * traction)
    return (TURNING_RADIUS / optimalRadius).coerceAtMost(1.0)
}
