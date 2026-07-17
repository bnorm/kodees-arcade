package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.atan2

class DriverState(
    val driver: WasmDriver,
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var checkpoint: Int = 0,
    var lap: Int = 0,
    var lapTimes: MutableList<Long> = mutableListOf(),
    var finished: Boolean = false,
) {
    fun angleTo(other: DriverState): Angle {
        return atan2(other.y - y, other.x - x)
    }

    fun distanceSq(other: DriverState): Double {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }
}
