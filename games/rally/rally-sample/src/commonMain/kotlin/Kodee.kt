package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*
import kotlin.random.Random

/**
 * Our racer! Kodee!
 *
 * This object implements the logic used to control our car during a race.
 */
object Kodee : Racer() {
    private lateinit var track: Track
    private var safety: Double = 0.0
    override fun onRace(track: Track) {
        this.track = track
        this.safety = Random.nextDouble(0.1)
    }

    override fun move(car: Car, controls: Controls) {
        val next = track.checkpoints[car.nextCheckpoint]
        val target = Point(
            x = next.start.x + (next.end.x - next.start.x) / 2,
            y = next.start.y + (next.end.y - next.start.y) / 2,
        )

        // Go a safe speed... for now!
        controls.throttle = 0.5 - safety

        // Figure out how to steer.
        val targetHeading = atan2(target.y - car.y, target.x - car.x)
        controls.steering = steeringToHeading(car, targetHeading)
    }

    private fun steeringToHeading(
        car: Car,
        targetHeading: Angle
    ): Double {
        val diff = (targetHeading - car.heading).toRelative()
        val sign = sign(diff)
        if (sign == 0.0) return 0.0

        val turn = getTurn(car.speed, steering = sign, traction = 1.0)
        return sign * (diff / turn).coerceAtMost(1.0)
    }
}
