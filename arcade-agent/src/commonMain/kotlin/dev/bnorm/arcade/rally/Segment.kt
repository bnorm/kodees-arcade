package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
class Segment(
    val start: Point,
    val end: Point,
)

val Segment.center: Point
    get() = Point(
        x = start.x + (end.x - start.x) / 2.0,
        y = start.y + (end.y - start.y) / 2.0,
    )

val Segment.length: Double
    get() {
        val dx = end.x - start.x
        val dy = end.y - start.y
        return sqrt(dx * dx + dy * dy)
    }
