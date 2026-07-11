package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.atan2

class RallyCarState(
    val name: String,
    val controls: DriverControlState,
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var checkpoint: Int = 0,
    var lap: Int = 0,
    var finished: Long? = null,
) {
    fun angleTo(other: RallyCarState): Angle {
        return atan2(other.y - y, other.x - x)
    }

    fun distanceSq(other: RallyCarState): Double {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }
}
