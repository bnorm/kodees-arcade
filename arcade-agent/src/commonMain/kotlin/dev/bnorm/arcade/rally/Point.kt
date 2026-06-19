package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable

@Serializable
data class Point(
    val x: Double,
    val y: Double,
) {
    companion object {
        val ZERO = Point(0.0, 0.0)
    }

    override fun toString(): String {
        return "($x, $y)"
    }
}
