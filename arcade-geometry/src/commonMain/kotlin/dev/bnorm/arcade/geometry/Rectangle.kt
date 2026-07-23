package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable

@Serializable
data class Rectangle(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) : Boundable {
    /**
     * Creates a [Rectangle] from the diagonal segment described by [start] and [end].
     */
    constructor(start: Point, end: Point) : this(
        minX = minOf(start.x, end.x),
        minY = minOf(start.y, end.y),
        maxX = maxOf(start.x, end.x),
        maxY = maxOf(start.y, end.y)
    )

    val width: Double
        get() = maxX - minX

    val height: Double
        get() = maxY - minY

    val center: Point
        get() = Point(
            x = (minX + maxX) / 2.0,
            y = (minY + maxY) / 2.0,
        )

    override val bounds: Rectangle
        get() = this

    operator fun contains(p: Point): Boolean {
        return p.x in minX..maxX && p.y in minY..maxY
    }

    override fun toString(): String {
        return "Rect[$minX..$maxX, $minY..$maxY]"
    }

}
