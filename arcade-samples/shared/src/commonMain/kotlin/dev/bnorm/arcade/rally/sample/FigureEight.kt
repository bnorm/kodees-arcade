package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*

object FigureEight : Racer() {
    private val epsilon = Angle.ofRadians(0.0001)
    private val turnAngle = Angle.HALF_CIRCLE
    private var targetHeading = turnAngle

    override fun move(car: Car, controls: Controls) {
        controls.throttle = 1.0

        // Do figure eights!
        val heading = car.velocity.heading
        val turn = getTurn(car.velocity.speed, steering = 1.0, traction = 1.0)
        val sign = sign(targetHeading)

        val diff = abs((targetHeading - heading + Angle.QUARTER_CIRCLE).toRelative())
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
