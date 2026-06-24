package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.toRelative

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

    val turn = getTurn(velocity.magnitude, steering = sign, traction = 1.0)
    return sign * (bearing / turn).coerceAtMost(1.0)
}
