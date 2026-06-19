package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.*
import kotlin.random.Random

object Snail : Racer() {
    private lateinit var track: Track
    private var safety: Double = 0.0
    override fun onRace(track: Track) {
        this.track = track
        this.safety = Random.nextDouble(0.1)
    }

    override fun move(car: Car, controls: Controls) {
        val next = track.checkpoints[car.nextCheckpoint]
        val target = next.center

        // Go a safe speed... for now!
        controls.throttle = 0.5 - safety

        // Figure out how to steer.
        val targetHeading = atan2(target.y - car.location.y, target.x - car.location.x)
        controls.steering = steeringToHeading(targetHeading, car.velocity)
    }
}
