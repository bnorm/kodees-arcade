import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Controls
import dev.bnorm.arcade.driver.Driver
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.MAX_STEER
import dev.bnorm.arcade.rally.MIN_STEER
import dev.bnorm.arcade.rally.getTurn
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
        val speed = car.velocity.magnitude
        val heading = car.velocity.angle
        val turn = (car.location.angleTo(target) - heading).toRelative()
        controls.steering = when {
            turn == Angle.ZERO -> 0.0
            speed == 0.0 -> sign(turn) * MAX_STEER
            else -> {
                val maxTurn = getTurn(speed, steering = MAX_STEER, traction = 1.0)
                (turn / maxTurn).coerceIn(MIN_STEER, MAX_STEER)
            }
        }
    }
}
