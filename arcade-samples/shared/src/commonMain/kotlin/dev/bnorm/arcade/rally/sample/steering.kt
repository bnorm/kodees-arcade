package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*

fun steeringToHeading(
    heading: Angle,
    velocity: Velocity
): Double {
    return steeringToBearing(
        bearing = (heading - velocity.heading).toRelative(),
        velocity = velocity,
    )
}

fun steeringToBearing(
    bearing: Angle,
    velocity: Velocity
): Double {
    val sign = sign(bearing)
    if (sign == 0.0) return 0.0

    val turn = getTurn(velocity.speed, steering = sign, traction = 1.0)
    return sign * (bearing / turn).coerceAtMost(1.0)
}
