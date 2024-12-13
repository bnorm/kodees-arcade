package dev.bnorm.arcade.cybertanks

import kotlin.math.abs

object Physics {
    const val ACCELERATION: Double = 1.0
    const val DECELERATION: Double = 2.0
    const val MAX_SPEED: Double = 8.0
    val MAX_TURN_RATE: Angle = Angle.ofDegrees(10.0)
    val MAX_TURN_SPEED_MULTIPLE: Angle = Angle.ofDegrees(-0.75)

    val GUN_TURN_RATE: Angle = Angle.ofDegrees(20.0)
    const val MIN_BULLET_POWER: Double = 0.1
    const val MAX_BULLET_POWER: Double = 3.0

    val RADAR_TURN_RATE: Angle = Angle.ofDegrees(45.0)
    const val RADAR_SCAN_RADIUS: Double = 1200.0

    const val ROBOT_HIT_DAMAGE: Double = 0.6
    const val ROBOT_HIT_BONUS: Double = 1.2

    fun getTurnRate(velocity: Double): Angle {
        return MAX_TURN_RATE + MAX_TURN_SPEED_MULTIPLE * abs(velocity)
    }

    fun getWallHitDamage(velocity: Double): Double {
        return (abs(velocity) / 2.0 - 1.0).coerceAtLeast(0.0)
    }

    fun getBulletDamage(bulletPower: Double): Double {
        return if (bulletPower > 1.0) {
            // 4.0 * bulletPower + 2.0 * (bulletPower - 1.0)
            6.0 * bulletPower - 2.0
        } else {
            4.0 * bulletPower
        }
    }

    fun getBulletHitBonus(bulletPower: Double): Double {
        return 3.0 * bulletPower
    }

    fun getBulletSpeed(bulletPower: Double): Double {
        return 20.0 - 3.0 * bulletPower.coerceIn(MIN_BULLET_POWER, MAX_BULLET_POWER)
    }

    fun getGunHeat(bulletPower: Double): Double {
        return 1.0 + bulletPower / 5.0
    }
}
