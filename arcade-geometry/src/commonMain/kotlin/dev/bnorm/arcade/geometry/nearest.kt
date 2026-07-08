package dev.bnorm.arcade.geometry

/**
 * Finds the nearest [Point] on the given [line] to [this] point.
 */
fun Point.nearest(line: Line): Point {
    if (line.isVertical) {
        return Point(line.b, y)
    } else if (line.isHorizontal) {
        return Point(x, line.b)
    } else {
        // y = m1 * x + b1
        val m1 = line.m
        val b1 = line.b

        // Normal line slop: m2 = -1 / m1
        val m2 = -1.0 / line.m
        // Normal line offset from line equation.
        val b2 = y - (m2 * x)

        // m1 * x + b1 = m2 * x + b2
        // x = (b2 - b1) / (m1 - m2)
        val x = (b1 - b2) / (m2 - m1)
        val y = m1 * x + b1
        return Point(x, y)
    }
}

/**
 * Finds the nearest [Point] on the given [segment] to [this] point.
 */
fun Point.nearest(segment: Segment): Point {
    return nearest(segment.toLine()).coerceIn(segment)
}

/**
 * Coerces [this] [Point] so it lies within the given [segment] bounds.
 * Assumes the given point already lies along the segment [Line].
 */
private fun Point.coerceIn(segment: Segment): Point {
    val minX = minOf(segment.start.x, segment.end.x)
    val maxX = maxOf(segment.start.x, segment.end.x)
    val minY = minOf(segment.start.y, segment.end.y)
    val maxY = maxOf(segment.start.y, segment.end.y)
    return Point(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
}
