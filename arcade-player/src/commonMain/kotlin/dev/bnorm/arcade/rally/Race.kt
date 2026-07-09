package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable

@Serializable
class Race(
    val track: Track,
    val laps: Int,
    // TODO other drivers?
)
