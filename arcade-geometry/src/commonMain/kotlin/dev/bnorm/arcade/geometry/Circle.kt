package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable

@Serializable
data class Circle(
    val center: Point,
    val radius: Double,
)

operator fun Circle.contains(p: Point) =
    center.distanceSquaredTo(p) <= radius * radius
