package dev.bnorm.arcade.driver.sample

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Controls
import dev.bnorm.arcade.driver.Driver
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.geometry.atan2
import dev.bnorm.arcade.geometry.center
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
