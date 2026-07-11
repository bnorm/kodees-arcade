package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.geometry.atan2
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Controls
import dev.bnorm.arcade.rally.Driver
import dev.bnorm.arcade.rally.Race
import dev.bnorm.arcade.rally.Track
import kotlin.random.Random

object Snail : Driver() {
    private lateinit var track: Track
    private var safety: Double = 0.0
    override fun onRace(race: Race) {
        this.track = race.track
        this.safety = Random.nextDouble(0.1)
    }

    override fun move(car: Car, controls: Controls) {
        val next = track.checkpoints[car.nextCheckpoint]
        val target = next.center

        // Go a safe speed... for now!
        controls.throttle = 0.4 - safety

        // Figure out how to steer.
        val targetHeading = atan2(target.y - car.location.y, target.x - car.location.x)
        controls.steering = steeringToHeading(targetHeading, car.velocity)
    }
}
