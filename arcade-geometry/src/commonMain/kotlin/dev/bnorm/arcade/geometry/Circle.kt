package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable

@Serializable
data class Circle(
    val center: Point,
    val radius: Double,
) : Boundable {
    operator fun contains(p: Point): Boolean {
        return center.distanceSquaredTo(p) <= radius * radius
    }

    override val bounds: Rectangle
        get() = Rectangle(
            minX = center.x - radius,
            minY = center.y - radius,
            maxX = center.x + radius,
            maxY = center.y + radius,
        )
}
