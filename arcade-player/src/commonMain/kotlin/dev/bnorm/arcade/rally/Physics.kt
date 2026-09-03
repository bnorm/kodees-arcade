package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.times
import dev.bnorm.arcade.geometry.toAbsolute
import kotlin.math.abs
import kotlin.math.sign

// Target about 30 updates per second to appear realistic.
private const val UPS_TARGET = 30.0

// Scale everything so there are about 6 pixels per meter.
// (Based on the track width (90px) being about 15 meters.)
// TODO consider adjusting the scale
//  - also make sure more things are dependent on this number
private const val SCALE = 6.0
const val TRACK_WIDTH = 15.0 * SCALE

// THROTTLE AND SPEED

// Measurements in meters per second squared (m/s^2).
const val ACCELERATION = 30.0 / (UPS_TARGET * UPS_TARGET) * SCALE // About 3Gs.
const val DECELERATION = 60.0 / (UPS_TARGET * UPS_TARGET) * SCALE // About 6Gs.
const val CORNERING = 65.0 / (UPS_TARGET * UPS_TARGET) * SCALE // About 6.5Gs.
const val BOOST_DEGRADE = 0.5 / (UPS_TARGET * UPS_TARGET) * SCALE

// Measurements in meters per second (m/s).
const val MAX_SPEED = 50.0 / UPS_TARGET * SCALE // 180 kph / ~112 mph.
const val MAX_SPEED_BOOST = 10.0 / UPS_TARGET * SCALE // 36 kph / ~22 mph.
const val MIN_SPEED = -10.0 / UPS_TARGET * SCALE // 36 kph / 22 mph.

// Limits
const val MIN_THROTTLE = -1.0 // -MAX_SPEED / MAX_SPEED
const val MAX_THROTTLE = 1.0 // MAX_SPEED / MAX_SPEED
const val MAX_BOOST_THROTTLE = (MAX_SPEED + MAX_SPEED_BOOST) / MAX_SPEED

fun simulateSpeed(speed: Double, boost: Double, throttle: Double): Double {
    // TODO should there be burnout?
    //  - hard acceleration causes speed increase to be lower?
    // TODO should there be skidding?
    //  - hard deceleration causes speed decrease to be lower?

    fun simulateAcceleration(currentSpeed: Double, targetSpeed: Double, overThrottle: Boolean): Double {
        // Let's deal with only positive target speed...
        if (targetSpeed < 0.0) return -simulateAcceleration(-currentSpeed, -targetSpeed, overThrottle)

        // Target speed is always >= 0
        return if (currentSpeed < 0.0) {
            if (-currentSpeed < DECELERATION) {
                // Need to decelerate and accelerate.
                val ratio = -currentSpeed / DECELERATION
                minOf(ratio * ACCELERATION, targetSpeed)
            } else {
                // Need to only decelerate.
                currentSpeed + DECELERATION
            }
        } else {
            if (targetSpeed > currentSpeed) {
                // Need to accelerate.
                minOf(currentSpeed + ACCELERATION, targetSpeed)
            } else if (currentSpeed >= MAX_SPEED && overThrottle) {
                // Let any speed boost slowly degrade.
                maxOf(currentSpeed - BOOST_DEGRADE, targetSpeed)
            } else {
                // Need to decelerate.
                maxOf(currentSpeed - DECELERATION, targetSpeed)
            }
        }
    }

    return when {
        throttle == 0.0 -> {
            simulateAcceleration(currentSpeed = speed, targetSpeed = 0.0, overThrottle = false)
        }

        throttle > 0.0 -> {
            val boostedThrottle = throttle.coerceAtMost((MAX_SPEED + boost) / MAX_SPEED)
            simulateAcceleration(
                currentSpeed = speed,
                targetSpeed = boostedThrottle * MAX_SPEED,
                overThrottle = throttle > boostedThrottle,
            )
        }

        else -> {
            simulateAcceleration(
                currentSpeed = speed,
                targetSpeed = throttle.coerceAtLeast(MIN_THROTTLE) * -MIN_SPEED,
                overThrottle = false,
            )
        }
    }
}

// STEERING AND HEADING

// Limits
const val MAX_STEER = 1.0
const val MIN_STEER = -1.0

// Measurements in meters.
const val MIN_TURNING_RADIUS = 5.0 * SCALE

fun getTurningRadius(speed: Double, traction: Double = 1.0): Double {
    return (speed * speed / (CORNERING * traction))
        .coerceAtLeast(MIN_TURNING_RADIUS)
}

fun getTurn(speed: Double, steering: Double, traction: Double = 1.0): Angle {
    if (steering == 0.0) return Angle.ZERO

    // Compute optimal and target turning radius.
    // Traction effects optimal turn radius by reducing cornering.
    val optimalRadius = getTurningRadius(speed, traction)
    val targetRadius = MIN_TURNING_RADIUS / abs(steering)

    val actualRadius = if (targetRadius >= optimalRadius) {
        targetRadius
    } else {
        // Compute understeer based on:
        //  - Current speed compared to max speed.
        //  - Excess steering over the optimal radius.
        targetRadius + sqr(optimalRadius - targetRadius) * sqr(speed / MAX_SPEED)
    }

    val turnSpeed = abs(speed).coerceAtLeast(ACCELERATION) // Allow a little turn in place...
    val turn = Angle.ofRadians(turnSpeed / actualRadius)
    val speedSign = if (sign(speed) == -1.0) -1.0 else 1.0
    return sign(steering) * speedSign * turn
}

fun simulateHeading(heading: Angle, speed: Double, steering: Double, traction: Double): Angle {
    return (heading + getTurn(speed, steering, traction)).toAbsolute()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun sqr(value: Double): Double = value * value
