package dev.bnorm.arcade.driver.sample

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.abs
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.MAX_SPEED
import dev.bnorm.arcade.rally.MAX_STEER
import dev.bnorm.arcade.rally.TURNING_RADIUS
import dev.bnorm.arcade.rally.getTurn
import dev.bnorm.arcade.rally.getTurningRadius
import kotlin.math.sqrt

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
    return getSteeringForTurn(bearing, velocity.magnitude)
}

fun getMaxSteering(speed: Double, traction: Double = 1.0): Double {
    val optimalRadius = getTurningRadius(speed, traction)
    // Cannot turn smaller than TURNING_RADIUS.
    if (optimalRadius <= TURNING_RADIUS) return MAX_STEER

    // Because of the understeer calculation,
    // we can actually push steering a little further,
    // to find an even smaller target radius!

    // turn = speed / actual
    // target = TURNING_RADIUS / steering
    // optimal = speed * speed / (C * traction) -- constant!
    // SR = speed / MAX_SPEED -- constant!

    // For 'target' <= 'optimal',
    // Find maximum 'turn' for 'steering',
    // -> Find min 'actual' for 'target' <= 'optimal'.

    // actual = target + (optimal - target)^2 * SR^2
    // actual = target + (optimal^2 - 2 * optimal * target + target^2) * SR^2 -- FOIL!
    // actual = (SR^2) * target^2 + (1 - 2 * SR^2 * optimal) * target + (SR^2 * optimal^2) -- quadratic!

    // For a given upward facing quadratic formula,
    // the smallest 'y' value is at the vertex,
    // given by the following formula for 'x':
    // y = A * x^2 + B * x + C
    // x = -B / (2 * A)

    // target = -(1 - 2 * SR^2 * optimal) / (2 * SR^2)
    // target = (2 * SR^2 * optimal - 1) / (2 * SR^2)
    // target = optimal - 1 / (2 * SR^2)

    // '1 / (2 * SR^2)' is always positive, therefore,
    // 'target' will always be less than 'optimal'!
    val speedRatio = speed / MAX_SPEED
    val targetRadius = optimalRadius - 1.0 / (2.0 * sqr(speedRatio))

    // Cannot turn smaller than TURNING_RADIUS.
    if (targetRadius <= TURNING_RADIUS) return MAX_STEER
    return TURNING_RADIUS / targetRadius
}

fun getMaxTurn(speed: Double, traction: Double = 1.0): Angle {
    // TODO would it be more efficient to simplify these formulas?
    val steering = getMaxSteering(speed, traction)
    return getTurn(speed, steering, traction)
}

fun getSteeringForTurn(turn: Angle, speed: Double, traction: Double = 1.0): Double {
    val sign = sign(turn)
    if (speed == 0.0) return sign * MAX_STEER

    val absTurn = abs(turn)
    val maxSteering = getMaxSteering(speed, traction)
    val maxTurn = getTurn(speed, maxSteering, traction)
    if (maxTurn == absTurn) {
        return sign * maxSteering
    } else if (maxTurn < absTurn) {
        // TODO we should maybe slow down?
        return sign * maxSteering
    }

    val optimalRadius = getTurningRadius(speed, traction)
    if (optimalRadius <= TURNING_RADIUS) {
        // Safe to turn without understeer!
        return turn / maxTurn
    }

    // There's going to be some understeer,
    // so determine the best possible steering,
    // based on the quadratic formula from 'getMaxSteering'.

    // actual = speed / turn -- constant!
    // actual = (SR^2) * target^2 + (1 - 2 * SR^2 * optimal) * target + (SR^2 * optimal^2) -- from 'getMaxSteering'!
    // 0 = (SR^2) * target^2 + (1 - 2 * SR^2 * optimal) * target + (SR^2 * optimal^2 - speed / turn)

    // Use the quadratic formula to find the zeros:
    // x = [-B -+ √(B^2 - 4 * A * C)] / (2 * A)

    val sr2 = sqr(speed / MAX_SPEED)
    val a = sr2
    val b = 1.0 - 2.0 * sr2 * optimalRadius
    val c = sr2 * sqr(optimalRadius) - speed / absTurn.radians

    // TODO do we want the + or - value?
    //  assuming we want the smaller value to maximize steering, use the negative?
    val sqrt = sqrt(sqr(b) - 4.0 * a * c)
    val targetRadius = (-b - sqrt) / (2.0 * a)
    return sign * TURNING_RADIUS / targetRadius
}

@Suppress("NOTHING_TO_INLINE")
private inline fun sqr(value: Double): Double = value * value
