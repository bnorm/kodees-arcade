package dev.bnorm.arcade.rally

import kotlin.math.abs
import kotlin.math.sign

// THROTTLE AND SPEED

const val MAX_THROTTLE = 1.0
const val MIN_THROTTLE = -1.0
const val ACCELERATION: Double = 0.1
const val DECELERATION: Double = 0.2
const val MAX_SPEED: Double = 8.0
const val MIN_SPEED: Double = -4.0

fun simulateSpeed(speed: Double, throttle: Double): Double {
    fun simulateAcceleration(actualSpeed: Double, targetSpeed: Double): Double {
        // Let's deal with only positive target speed...
        if (targetSpeed < 0.0) return -simulateAcceleration(-actualSpeed, -targetSpeed)

        // Target speed is always >= 0
        return if (actualSpeed < 0.0) {
            if (-actualSpeed < DECELERATION) {
                // Need to decelerate and accelerate.
                val ratio = -actualSpeed / DECELERATION
                minOf(ratio + ACCELERATION, targetSpeed)
            } else {
                // Need to only decelerate.
                actualSpeed + DECELERATION
            }
        } else {
            if (targetSpeed > actualSpeed) {
                // Need to accelerate.
                minOf(actualSpeed + ACCELERATION, targetSpeed)
            } else {
                // Need to decelerate.
                maxOf(actualSpeed - DECELERATION, targetSpeed)
            }
        }
    }

    return when {
        throttle == 0.0 -> simulateAcceleration(speed, 0.0)
        throttle > 0.0 -> simulateAcceleration(speed, throttle * MAX_SPEED)
        else -> simulateAcceleration(speed, -throttle * MIN_SPEED)
    }
}

// STEERING AND HEADING

const val MAX_STEER = 1.0
const val MIN_STEER = -1.0
val MAX_TURN_RATE = Angle.ofDegrees(8.0)
val MAX_TURN_SPEED_MULTIPLE = Angle.ofDegrees(0.75)

fun getTurn(speed: Double, steering: Double, traction: Double): Angle {
    val speed = abs(speed)
    val targetTurn = MAX_TURN_RATE * abs(steering)

    // TODO is there a minimum speed for turn as well?
    //  zero speed should be no turn
    //  high speeds (plus strong steering and low traction) should result in less turn
    //  so is there a "sweet spot" for speed to get all of the turn possible?
    //  at this sweet spot, should there still be a penalty for low traction?

    // TODO there should be a penalty based on strong steering.
    //  at slow speeds, strong steering doesn't matter.
    //  at high speeds, strong steering should cause "slipage" (lower turn).
    //  does this mean exponential penalty to turn based on speed and steering?

    // TODO traction should limit turn in a more interesting way.
    //  at slow speeds, traction shouldn't matter.
    //  at high speeds with strong steering, low traction should cause "slipage" (lower turn).
    //  does this mean exponential penalty to turn based on speed, steering, and traction?
    //  does this double up with the normal speed+steering penalty?

    // TODO what about backwards?!
    //  does backwards speed have a completely different set of rules?
    //  is there no penalty for driving backwards?

    // Overdrive starts at 25% of max speed and means a turning penalty.
    val overdrive = (speed - MAX_SPEED / 4.0)
    val speedPenalty = (MAX_TURN_SPEED_MULTIPLE * overdrive).coerceAtLeast(Angle.ZERO)

    // Traction directly impacts turning radius.
    // But only if the speed ratio is greater than available traction.
    val tractionPenalty = 1.0 - (speed / MAX_SPEED - traction).coerceAtLeast(0.0)

    // This is the maximum turn that is available given the speed and traction.
    val maxTurn = (MAX_TURN_RATE - speedPenalty) * tractionPenalty

    // If the target turn is greater than the max turn => understeer.
    val understeerPenalty = (targetTurn - maxTurn).coerceIn(Angle.ZERO, abs(targetTurn))
    return sign(steering) * (targetTurn - understeerPenalty)
}

fun simulateHeading(heading: Angle, speed: Double, steering: Double, traction: Double): Angle {
    return (heading + getTurn(speed, steering, traction)).toAbsolute()
}
