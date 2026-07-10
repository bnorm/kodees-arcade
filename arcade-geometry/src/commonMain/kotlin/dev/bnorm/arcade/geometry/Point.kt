package dev.bnorm.arcade.geometry

import kotlin.math.sqrt
import kotlinx.serialization.Serializable

@Serializable
data class Point(
    val x: Double,
    val y: Double,
) {
    companion object {
        val ZERO = Point(0.0, 0.0)
    }

    operator fun plus(point: Point): Point {
        return plus(point.x, point.y)
    }

    fun plus(x: Double, y: Double): Point {
        return Point(this.x + x, this.y + y)
    }

    operator fun minus(point: Point): Point {
        return minus(point.x, point.y)
    }

    fun minus(x: Double, y: Double): Point {
        return Point(this.x - x, this.y - y)
    }

    operator fun times(scalar: Double): Point {
        return Point(this.x * scalar, this.y * scalar)
    }

    operator fun div(scalar: Double): Point {
        return Point(this.x / scalar, this.y / scalar)
    }

    /**
     * Calculate the distance *squared* to another point.
     * This function avoids a costly [sqrt] call
     * which may help optimize certain calculations.
     */
    fun distanceSquaredTo(other: Point): Double {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }

    /**
     * Calculates the exact distance to another point.
     */
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

operator fun Point.plus(vector: Vector): Point {
    return plus(vector.angle, vector.magnitude)
}

fun Point.plus(angle: Angle, magnitude: Double): Point {
    return Point(x + cos(angle) * magnitude, y + sin(angle) * magnitude)
}

operator fun Point.minus(vector: Vector): Point {
    return minus(vector.angle, vector.magnitude)
}

fun Point.minus(angle: Angle, magnitude: Double): Point {
    return Point(x - cos(angle) * magnitude, y - sin(angle) * magnitude)
}

operator fun Double.times(point: Point): Point {
    return point.times(this)
}

operator fun Double.div(point: Point): Point {
    return point.div(this)
}
