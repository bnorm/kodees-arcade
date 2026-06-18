package dev.bnorm.arcade.rally

import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.roundToLong

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

fun abs(angle: Angle) = Angle.ofRadians(kotlin.math.abs(angle.radians))
fun sign(angle: Angle) = kotlin.math.sign(angle.radians)

fun sin(angle: Angle) = kotlin.math.sin(angle.radians)
fun cos(angle: Angle) = kotlin.math.cos(angle.radians)
fun tan(angle: Angle) = kotlin.math.tan(angle.radians)

fun asin(x: Double): Angle = Angle.ofRadians(kotlin.math.asin(x))
fun acos(x: Double): Angle = Angle.ofRadians(kotlin.math.acos(x))
fun atan(x: Double): Angle = Angle.ofRadians(kotlin.math.atan(x))
fun atan2(y: Double, x: Double): Angle = Angle.ofRadians(kotlin.math.atan2(y, x))

operator fun Double.times(angle: Angle) = Angle.ofRadians(this * angle.radians)
