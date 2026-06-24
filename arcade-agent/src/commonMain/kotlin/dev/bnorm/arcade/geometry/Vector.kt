package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
class Vector(
    val angle: Angle,
    val magnitude: Double,
) {
    companion object {
        val ZERO = Vector(Angle.ZERO, 0.0)
        fun unit(angle: Angle = Angle.ZERO): Vector = Vector(angle, 1.0)
    }
}

fun Vector.toPoint(): Point {
    return Point.ZERO.plus(this)
}

fun Vector.plus(other: Vector): Vector {
    // https://math.stackexchange.com/questions/1365622/adding-two-polar-vectors

    val r1 = magnitude
    val theta1 = angle
    val r2 = other.magnitude
    val theta2 = other.angle

    val a = theta2 - theta1
    val u = r2 * cos(a)
    val r = sqrt(r1 * r1 + r2 * r2 + 2 * r1 * u)
    val theta = theta1 + atan2(r2 * sin(a), r1 + u)

    return Vector(theta, r)
}
