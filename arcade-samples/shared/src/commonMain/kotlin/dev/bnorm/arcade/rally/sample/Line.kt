package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.Angle
import dev.bnorm.arcade.rally.Point
import dev.bnorm.arcade.rally.tan
import dev.bnorm.arcade.rally.toAbsolute

class Line(
    val m: Double,
    val b: Double,
) {
    // For a vertical line, formula is x = b
    val vertical: Boolean get() = m.isNaN()
}

fun Line(p1: Point, p2: Point): Line {
    // Vertical line
    if (p1.x == p2.x) return Line(Double.NaN, p1.x)

    // y = m * x + b -> b = y - m * x
    // y1 - m * x1 = y2 - m * x2
    // y1 - y2 = m * (x1 - x2)
    // m = (y1 - y2) / (x1 - x2)
    val m = (p1.y - p2.y) / (p1.x - p2.x)

    // y = m * x + b -> m = (y - b) / x
    // b = y1 - m * x1
    val b = p1.y - m * p1.x

    return Line(m, b)
}

fun Line(p: Point, angle: Angle): Line {
    // Vertical line
    val absolute = angle.toAbsolute()
    if (absolute % Angle.HALF_CIRCLE == Angle.QUARTER_CIRCLE) return Line(Double.NaN, p.x)

    // y = m * x + b
    val m = tan(absolute)
    val b = p.y - (m * p.x)
    return Line(m, b)
}

fun Line.f(x: Double) = Point(x, m * x + b)

operator fun Line.contains(p: Point): Boolean =
    if (vertical) p.x == b else m * p.x + b == p.y

infix fun Line.intersect(line: Line): Point? {
    if (this == line) return null // the same line
    if (this.vertical && line.vertical) return null // both vertical and not equal

    // If one line is vertical, intersection is easily calculated
    if (this.vertical) return line.f(this.b)
    if (line.vertical) return this.f(line.b)

    // y = m1 * x + b1
    // y = m2 * x + b2
    // m1 * x + b1 = m2 * x + b2
    // x = (b2 - b1) / (m1 - m2)

    return this.f((line.b - b) / (m - line.m))
}
