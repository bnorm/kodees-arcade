package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

@Serializable
@JvmInline
value class Angle private constructor(
    val radians: Double
) : Comparable<Angle> {
    val degrees: Double get() = radians.toDegrees()

    companion object {
        val ZERO = Angle(0.0)
        val QUARTER_CIRCLE = Angle(PI / 2)
        val HALF_CIRCLE = Angle(PI)
        val FULL_CIRCLE = Angle(2 * PI)

        val POSITIVE_INFINITY = Angle(Double.POSITIVE_INFINITY)
        val NEGATIVE_INFINITY = Angle(Double.NEGATIVE_INFINITY)

        fun ofRadians(value: Double): Angle = Angle(value)
        fun ofDegrees(value: Double): Angle = Angle(value.toRadians())
    }

    operator fun plus(angle: Angle): Angle = ofRadians(radians + angle.radians)
    operator fun minus(angle: Angle): Angle = ofRadians(radians - angle.radians)
    operator fun unaryMinus(): Angle = ofRadians(-radians)

    operator fun times(scalar: Double): Angle = ofRadians(radians * scalar)

    operator fun div(scalar: Double): Angle = ofRadians(radians / scalar)
    operator fun div(angle: Angle): Double = radians / angle.radians
    operator fun rem(angle: Angle): Angle = ofRadians(radians % angle.radians)

    override fun compareTo(other: Angle): Int = radians.compareTo(other.radians)

    override fun toString(): String {
        return "${((degrees * 10.0).roundToLong() / 10.0)}°"
    }
}

fun Double.toRadians(): Double = this * PI / 180.0
fun Double.toDegrees(): Double = this * 180.0 / PI

fun Angle.toAbsolute(): Angle {
    val normalized = radians % (2 * PI)
    return Angle.ofRadians(if (normalized >= 0.0) normalized else normalized + 2 * PI)
}

fun Angle.toRelative(): Angle {
    val normalized = (radians + PI) % (2 * PI)
    return Angle.ofRadians(if (normalized >= 0.0) normalized - PI else normalized + PI)
}

fun abs(angle: Angle) = Angle.ofRadians(abs(angle.radians))
fun sign(angle: Angle) = sign(angle.radians)

fun sin(angle: Angle) = sin(angle.radians)
fun cos(angle: Angle) = cos(angle.radians)
fun tan(angle: Angle) = tan(angle.radians)

fun asin(x: Double): Angle = Angle.ofRadians(asin(x))
fun acos(x: Double): Angle = Angle.ofRadians(acos(x))
fun atan(x: Double): Angle = Angle.ofRadians(atan(x))
fun atan2(y: Double, x: Double): Angle = Angle.ofRadians(atan2(y, x))

operator fun Double.times(angle: Angle) = Angle.ofRadians(this * angle.radians)
