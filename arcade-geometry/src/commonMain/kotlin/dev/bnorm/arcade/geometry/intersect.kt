@file:Suppress("NOTHING_TO_INLINE")

package dev.bnorm.arcade.geometry

import kotlin.math.sqrt

private inline fun sqr(d: Double): Double {
    return d * d
}

infix fun Rectangle.intersect(circle: Circle): Set<Point> {
    val points = mutableSetOf<Point>()

    // circle: (x - x0)² + (y - y0)² = r²
    // x = x0 ± √(r² - (y - y0)²)
    // y = y0 ± √(r² - (x - x0)²)

    val x0 = circle.center.x
    val y0 = circle.center.y
    val r2 = sqr(circle.radius)

    fun solveY(x: Double) {
        val ySum2 = (r2 - sqr(x - x0))
        if (ySum2 == 0.0) {
            if (y0 in yRange) points.add(Point(x, y0))
        } else if (ySum2 > 0.0) {
            val ySum = sqrt(ySum2)
            val yTop = y0 + ySum
            val yBottom = y0 - ySum
            if (yTop in yRange) points.add(Point(x, yTop))
            if (yBottom in yRange) points.add(Point(x, yBottom))
        }
    }

    fun solveX(y: Double) {
        val xSum2 = (r2 - sqr(y - y0))
        if (xSum2 == 0.0) {
            if (x0 in xRange) points.add(Point(x0, y))
        } else if (xSum2 > 0.0) {
            val xSum = sqrt(xSum2)
            val xRight = x0 + xSum
            val xLeft = x0 - xSum
            if (xRight in xRange) points.add(Point(xRight, y))
            if (xLeft in xRange) points.add(Point(xLeft, y))
        }
    }

    solveY(xRange.start) // left
    solveY(xRange.endInclusive) // right
    solveX(yRange.start) // bottom
    solveX(yRange.endInclusive) // top

    return points
}

inline infix fun Circle.intersect(rectangle: Rectangle) = rectangle.intersect(this)

// Determine the tangent lines from a source point which touch a circle

infix fun Circle.tangents(p: Point): Set<Line> {
    val dist2 = center.distanceSquaredTo(p)
    if (dist2 < radius * radius) return emptySet()

    val angle = center.angleTo(p)
    if (dist2 == radius * radius) return setOf(Line(p, angle + Angle.QUARTER_CIRCLE))

    val delta = acos(radius / sqrt(dist2))
    return setOf(
        Line(p, center.plus(angle + delta, radius)),
        Line(p, center.plus(angle - delta, radius)),
    )
}

inline infix fun Point.tangents(circle: Circle) =
    circle.tangents(this)

infix fun Circle.tangentPoints(p: Point): Set<Point> {
    val dist2 = center.distanceSquaredTo(p)
    if (dist2 < radius * radius) return emptySet()
    if (dist2 == radius * radius) return setOf(p)

    val angle = center.angleTo(p)
    val delta = acos(radius / sqrt(dist2))
    return setOf(
        center.plus(angle + delta, radius),
        center.plus(angle - delta, radius),
    )
}

inline infix fun Point.tangentPoints(circle: Circle) =
    circle.tangentPoints(this)

//

fun Circle.intersectHorizontal(y: Double): Set<Point> {
    // circle: (x - x0)² + (y - y0)² = r²
    // x = x0 ± √(r² - (y - y0)²)
    val prime = radius * radius - sqr(y - center.y)
    return when {
        prime < 0.0 -> emptySet()
        prime == 0.0 -> setOf(Point(center.x, y))
        else -> setOf(
            Point(center.x + sqrt(prime), y),
            Point(center.x - sqrt(prime), y),
        )
    }
}

fun Circle.intersectVertical(x: Double): Set<Point> {
    // circle: (x - x0)² + (y - y0)² = r²
    // y = y0 ± √(r² - (x - x0)²)
    val prime = radius * radius - sqr(x - center.x)
    return when {
        prime < 0.0 -> emptySet()
        prime == 0.0 -> setOf(Point(x, center.y))
        else -> setOf(
            Point(x, center.y + sqrt(prime)),
            Point(x, center.y - sqrt(prime)),
        )
    }
}

infix fun Circle.intersect(line: Line): Set<Point> {
    if (line.isVertical) {
        return intersectVertical(line.b)
    } else if (line.isHorizontal) {
        return intersectHorizontal(line.b)
    }

    // circle: (x - x0)² + (y - y0)² = r²
    // line: y = m * x + b

    // (x - x0)² + [m * x + (b - y0)]² = r²
    // [x² + 2 * x * -x0 + x0²] + [(m * x)² + 2 * m * x * (b - y0) + (b - y0)²] = r²
    // [m² + 1] * x² + [2 * -x0 + m * (b - y0)] * x + [x0² + (b - y0)² - r²] = 0

    // quadratic: a * x² + b * x + c = 0
    // x = (-b ± √(b² - 4 * a * c)) / (2 * a)

    val slope = line.m
    val elevation = line.b
    val x0 = center.x
    val y0 = center.y

    val c = x0 * x0 + sqr(elevation - y0) - radius * radius
    val b = 2 * (-x0 + slope * (elevation - y0)) // 2
    val a = slope * slope + 1 // 2

    val prime = sqr(b) - 4 * a * c
    return when {
        prime < 0.0 -> emptySet()
        prime == 0.0 -> setOf(line.intersectVertical(-b / (2 * a))!!)
        else -> {
            setOf(
                line.intersectVertical((-b + sqrt(prime)) / (2 * a))!!,
                line.intersectVertical((-b - sqrt(prime)) / (2 * a))!!,
            )
        }
    }
}

inline infix fun Line.intersect(circle: Circle) =
    circle.intersect(this)

//
//
//

infix fun Line.intersect(segment: Segment): Point? {
    val x1 = segment.start.x
    val y1 = segment.start.y
    val x2 = segment.end.x
    val y2 = segment.end.y

    if (isVertical) {
        if (x1 == x2) return null

        val x = b
        return if (x1 < x2 && x in x1..x2 || x in x2..x1) TODO("linear average")
        else null
    }

    if (x1 == x2) {

        val y = m * x1 + b
        return if (y1 < y2 && y in y1..y2 || y in y2..y1) Point(x1, y)
        else null
    }

    // y = m * x + b -> b = y - m * x
    // y1 - m * x1 = y2 - m * x2
    // y1 - y2 = m * (x1 - x2)
    // m = (y1 - y2) / (x1 - x2)
    val m = (y1 - y2) / (x1 - x2)

    // y = m * x + b -> m = (y - b) / x
    // b = y1 - m * x1
    val b = y1 - m * x1

    // y = m1 * x + b1
    // y = m2 * x + b2
    // m1 * x + b1 = m2 * x + b2
    // x = (b2 - b1) / (m1 - m2)
    val x = (this.b - b) / (m - this.m)
    val y = this.m * x + this.b

    return if (y - m * x == y1 - m * x1) Point(x, y)
    else null
}

//
//
//

infix fun Rectangle.intersect(segment: Segment): List<Point> {
    val contains1 = contains(segment.start)
    val contains2 = contains(segment.start)

    // Both points are inside so intersection is impossible
    if (contains1 && contains2) return emptyList()

    val c1 = Point(xRange.start, yRange.start)
    val c2 = Point(xRange.endInclusive, yRange.start)
    val c3 = Point(xRange.endInclusive, yRange.endInclusive)
    val c4 = Point(xRange.start, yRange.endInclusive)

    return listOfNotNull(
        segment intersect Segment(c1, c2),
        segment intersect Segment(c2, c3),
        segment intersect Segment(c3, c4),
        segment intersect Segment(c4, c1),
    )
}
