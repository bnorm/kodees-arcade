package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*

/**
 * Our racer! This object implements the logic used to control the car during a race.
 */
object Kodee : Racer() {
    private val epsilon = Angle.ofRadians(0.0001)
    private val turnAngle = Angle.HALF_CIRCLE
    private var targetHeading = turnAngle

    override fun move(car: Car, controls: Controls) {
        controls.throttle = 1.0

        // Do figure eights!
        val heading = car.heading
        val turn = getTurn(car.speed, steering = 1.0, traction = 1.0)
        val sign = sign(targetHeading)

        val diff = abs((targetHeading - heading).toRelative())
        if (diff < epsilon) {
            // Reverse turning directions.
            targetHeading *= -1.0
            controls.steering = -sign
        } else if (diff > turn) {
            // Continue turning in the same direction.
            controls.steering = sign
        } else {
            // Slow turning down a little to hit target heading.
            controls.steering = sign * (diff / turn)
        }
    }
}
