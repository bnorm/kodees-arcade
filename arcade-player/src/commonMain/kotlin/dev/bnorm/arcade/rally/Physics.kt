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

// THROTTLE AND SPEED

// Limits
const val MAX_THROTTLE = 1.0
const val MIN_THROTTLE = -1.0

// Measurements in meters per second squared (m/s^2).
const val ACCELERATION = 30.0 / (UPS_TARGET * UPS_TARGET) * SCALE
const val DECELERATION = 60.0 / (UPS_TARGET * UPS_TARGET) * SCALE
const val CORNERING = 65.0 / (UPS_TARGET * UPS_TARGET) * SCALE
const val BOOST_DEGRADE = 0.5 / (UPS_TARGET * UPS_TARGET) * SCALE

// Measurements in meters per second (m/s).
const val MAX_SPEED = 50.0 / UPS_TARGET * SCALE
const val MAX_SPEED_BOOST = 5.0 / UPS_TARGET * SCALE
const val MIN_SPEED = -10.0 / UPS_TARGET * SCALE

fun simulateSpeed(speed: Double, boost: Double, throttle: Double): Double {
    // TODO should there be burnout?
    //  - hard acceleration causes speed increase to be lower?
    // TODO should there be skidding?
    //  - hard deceleration causes speed decrease to be lower?

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
            } else if (actualSpeed >= MAX_SPEED && targetSpeed >= MAX_SPEED) {
                // Let any speed boost slowly degrade.
                maxOf(actualSpeed - BOOST_DEGRADE, targetSpeed)
            } else {
                // Need to decelerate.
                maxOf(actualSpeed - DECELERATION, targetSpeed)
            }
        }
    }

    return when {
        throttle == 0.0 -> simulateAcceleration(speed, 0.0)
        throttle > 0.0 -> simulateAcceleration(speed, throttle * MAX_SPEED + boost)
        else -> simulateAcceleration(speed, -throttle * MIN_SPEED)
    }
}

// STEERING AND HEADING

// Limits
const val MAX_STEER = 1.0
const val MIN_STEER = -1.0

// Measurements in meters.
const val TURNING_RADIUS = 5.0 * SCALE

fun getTurn(speed: Double, steering: Double, traction: Double = 1.0): Angle {
    if (steering == 0.0) return Angle.ZERO

    // Compute optimal and target turning radius.
    // Traction effects optimal turn radius by reducing cornering.
    val optimalRadius = speed * speed / (CORNERING * traction)
    val targetRadius = TURNING_RADIUS / abs(steering)

    // Compute oversteer based on:
    //  - Current speed.
    //  - Excess steering over the optimal radius.
    val speedRatio = speed / (MAX_SPEED + MAX_SPEED_BOOST)
    val oversteer = (optimalRadius - targetRadius).coerceAtLeast(0.0)
    val actualRadius = targetRadius + oversteer * oversteer * speedRatio * speedRatio

    val theta = Angle.ofRadians(abs(speed) / actualRadius)
    return sign(steering) * theta
}

fun simulateHeading(heading: Angle, speed: Double, steering: Double, traction: Double): Angle {
    return (heading + getTurn(speed, steering, traction)).toAbsolute()
}
