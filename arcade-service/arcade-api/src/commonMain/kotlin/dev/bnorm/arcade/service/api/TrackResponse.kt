package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

// TODO rename to circuit?
@Serializable
class TrackResponse(
    val id: TrackId,
    val name: String,
)
