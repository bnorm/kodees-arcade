package dev.bnorm.arcade.geometry

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

operator fun Segment.contains(p: Point): Boolean {
    return contains(p.x, p.y)
}

fun Segment.contains(x: Double, y: Double): Boolean {
    // Vertical segment
    if (start.x == end.x) return x == start.x
    if (x !in minOf(start.x, end.x)..maxOf(start.x, end.x) ||
        y !in minOf(start.y, end.y)..maxOf(start.y, end.y)
    ) return false

    // y = m * x + b -> b = y - m * x
    // y1 - m * x1 = y2 - m * x2
    // y1 - y2 = m * (x1 - x2)
    // m = (y1 - y2) / (x1 - x2)
    val m = (start.y - end.y) / (start.x - end.x)

    // y = m * x + b
    return y - m * x == start.y - m * start.x
}

infix fun Segment.intersect(segment: Segment): Point? {
    // https://www.geeksforgeeks.org/program-for-point-of-intersection-of-two-lines/

    // a1 * x + b1 * y = c1
    // a2 * x + b2 * y = c2

    val a1 = end.y - start.y
    val b1 = start.x - end.x

    val a2 = segment.end.y - segment.start.y
    val b2 = segment.start.x - segment.end.x

    val determinant = a1 * b2 - a2 * b1

    return if (determinant == 0.0) null // parallel
    else {
        val c1 = a1 * start.x + b1 * start.y
        val c2 = a2 * segment.start.x + b2 * segment.start.y

        val x = (b2 * c1 - b1 * c2) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant

        if (
            x in minOf(start.x, end.x)..maxOf(start.x, end.x) &&
            y in minOf(start.y, end.y)..maxOf(start.y, end.y) &&
            x in minOf(segment.start.x, segment.end.x)..maxOf(segment.start.x, segment.end.x) &&
            y in minOf(segment.start.y, segment.end.y)..maxOf(segment.start.y, segment.end.y)
        ) {
            Point(x, y)
        } else {
            null
        }
    }
}

fun Segment.toLine(): Line {
    return Line(start, end)
}
