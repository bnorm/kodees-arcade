package dev.bnorm.arcade.rally

import kotlin.math.abs

// THROTTLE AND SPEED

const val MAX_THROTTLE = 1.0
const val MIN_THROTTLE = -1.0
const val ACCELERATION: Double = 1.0
const val DECELERATION: Double = 2.0
const val MAX_SPEED: Double = 8.0
const val MIN_SPEED: Double = -4.0

fun simulateSpeed(speed: Double, throttle: Double): Double {
    fun simulateAcceleration(actualSpeed: Double, targetSpeed: Double): Double {
        // Let's deal with only positive target speed...
        if (targetSpeed < 0.0) return -simulateAcceleration(-actualSpeed, -targetSpeed)

        // Target speed is always >= 0
        return if (actualSpeed >= 0.0) {
            // Need to accelerate.
            minOf(actualSpeed + ACCELERATION, targetSpeed)
        } else if (-actualSpeed < DECELERATION) {
            // Need to decelerate and accelerate.
            val ratio = -actualSpeed / DECELERATION
            minOf(ratio * ACCELERATION, targetSpeed)
        } else {
            // Need to only decelerate.
            actualSpeed + DECELERATION
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
val MAX_TURN_RATE = Angle.ofDegrees(4.0)
val MAX_TURN_SPEED_MULTIPLE = Angle.ofDegrees(0.5)

fun getTurn(speed: Double, steering: Double, traction: Double): Angle {
    val speed = abs(speed)

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

    val overdrive = (speed - MAX_SPEED / 2.0)
    val penalty = (MAX_TURN_SPEED_MULTIPLE * overdrive).coerceAtLeast(Angle.ZERO)
    return (MAX_TURN_RATE - penalty) * steering * traction
}

fun simulateHeading(heading: Angle, speed: Double, steering: Double, traction: Double): Angle {
    return (heading + getTurn(speed, steering, traction)).toAbsolute()
}
