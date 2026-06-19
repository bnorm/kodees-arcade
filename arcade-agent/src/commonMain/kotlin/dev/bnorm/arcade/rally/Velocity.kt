package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable

@Serializable
class Velocity(
    val heading: Angle,
    val speed: Double,
)
