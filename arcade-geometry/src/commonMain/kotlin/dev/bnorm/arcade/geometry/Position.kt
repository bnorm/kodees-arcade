package dev.bnorm.arcade.geometry

import kotlinx.serialization.Serializable

@Serializable
class Position(
    val location: Point,
    val heading: Angle,
)
