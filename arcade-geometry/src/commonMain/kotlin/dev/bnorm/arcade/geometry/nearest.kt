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
        // Mathematically guaranteed they will intersect.
        val normal = Line(this, -1.0 / line.m)
        return normal.intersect(line)!!
    }
}

/**
 * Finds the nearest [Point] on the given [segment] to [this] point.
 */
fun Point.nearest(segment: Segment): Point {
    return nearest(segment.toLine()).coerceIn(segment)
}

private fun Point.coerceIn(segment: Segment): Point {
    val minX = minOf(segment.start.x, segment.end.x)
    val maxX = maxOf(segment.start.x, segment.end.x)
    val minY = minOf(segment.start.y, segment.end.y)
    val maxY = maxOf(segment.start.y, segment.end.y)
    return Point(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
}
