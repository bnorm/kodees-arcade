package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class Point(
    val x: Double,
    val y: Double,
) {
    companion object {
        val ZERO = Point(0.0, 0.0)
    }

    fun distanceSquaredTo(other: Point): Double {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }

    fun distanceTo(other: Point): Double {
        return sqrt(distanceSquaredTo(other))
    }

    fun angleTo(other: Point): Angle {
        return atan2(other.y - y, other.x - x)
    }

    override fun toString(): String {
        return "($x, $y)"
    }
}

fun Point.toVector(origin: Point = Point.ZERO): Vector {
    return Vector(origin.angleTo(this), origin.distanceTo(this))
}

operator fun Point.plus(point: Point): Point {
    return plus(point.x, point.y)
}

fun Point.plus(x: Double, y: Double): Point {
    return Point(this.x + x, this.y + y)
}

operator fun Point.plus(vector: Vector): Point {
    return plus(vector.angle, vector.magnitude)
}

fun Point.plus(angle: Angle, magnitude: Double): Point {
    return Point(x + cos(angle) * magnitude, y + sin(angle) * magnitude)
}
