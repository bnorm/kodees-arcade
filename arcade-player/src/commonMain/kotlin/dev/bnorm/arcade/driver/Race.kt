package dev.bnorm.arcade.driver

import kotlinx.serialization.Serializable

@Serializable
class Race(
    val track: Track,
    val laps: Int,
    // TODO other drivers?
)
