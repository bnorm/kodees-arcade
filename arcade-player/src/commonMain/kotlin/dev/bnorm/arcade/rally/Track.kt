package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import kotlinx.serialization.Serializable

@Serializable
class Track(
    val width: Double,
    val height: Double,
    val checkpoints: List<Segment>,
    val positions: List<Position>,
)
