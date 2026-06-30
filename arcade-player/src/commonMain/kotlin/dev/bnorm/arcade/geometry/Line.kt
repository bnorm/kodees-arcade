package dev.bnorm.arcade.geometry

class Line(
    val m: Double,
    val b: Double,
) {
    // For a vertical line, formula is x = b
    val isVertical: Boolean get() = m.isNaN()
    val isHorizontal: Boolean get() = m == 0.0

    override fun equals(other: Any?): Boolean {
        return this === other || other is Line &&
                m == other.m && b == other.b
    }

    override fun hashCode(): Int {
        return 31 * m.hashCode() + b.hashCode()
    }
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

fun Line.intersectVertical(x: Double): Point? {
    if (isVertical) return null
    return Point(x, m * x + b)
}

operator fun Line.contains(p: Point): Boolean =
    if (isVertical) p.x == b else m * p.x + b == p.y

infix fun Line.intersect(line: Line): Point? {
    if (this == line) return null // the same line
    if (this.isVertical && line.isVertical) return null // both vertical and not equal

    // If one line is vertical, intersection is easily calculated
    if (this.isVertical) return line.intersectVertical(this.b)
    if (line.isVertical) return this.intersectVertical(line.b)

    // y = m1 * x + b1
    // y = m2 * x + b2
    // m1 * x + b1 = m2 * x + b2
    // x = (b2 - b1) / (m1 - m2)

    return this.intersectVertical((line.b - b) / (m - line.m))
}
