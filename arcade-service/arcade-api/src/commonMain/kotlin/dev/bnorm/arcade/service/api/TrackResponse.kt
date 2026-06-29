package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class TrackResponse(
    val id: TrackId,
    val name: String,
)
