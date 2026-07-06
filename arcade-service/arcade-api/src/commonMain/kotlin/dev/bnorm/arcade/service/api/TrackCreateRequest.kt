package dev.bnorm.arcade.service.api

import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import kotlinx.serialization.Serializable

@Serializable
class TrackCreateRequest(
    val name: String,
    val width: Double,
    val height: Double,
    val checkpoints: List<Segment>,
    val positions: List<Position>,
)
