import dev.bnorm.arcade.rally.CORNERING
import dev.bnorm.arcade.rally.MAX_SPEED
 import dev.bnorm.arcade.rally.MIN_TURNING_RADIUS
import kotlin.math.sqrt

val TURNING_RADIUS_SPEED = sqrt(MIN_TURNING_RADIUS * CORNERING)

fun getMaxSpeed(radius: Double, traction: Double = 1.0): Double {
    // Based on formula for 'getTurningRadius':
    // radius = speed * speed / (CORNERING * traction) -- min TURNING_RADIUS
    return when {
        radius <= MIN_TURNING_RADIUS -> when (traction) {
            1.0 -> TURNING_RADIUS_SPEED // Avoids a 'sqrt' call.
            else -> sqrt(MIN_TURNING_RADIUS * CORNERING * traction)
        }

        else -> sqrt(radius * CORNERING * traction)
    }
}

fun getMaxThrottle(radius: Double, traction: Double = 1.0): Double {
    // TODO what if a speed boost is available?
    return (getMaxSpeed(radius, traction) / MAX_SPEED)
        .coerceAtMost(1.0)
}
