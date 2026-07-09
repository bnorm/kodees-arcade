package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class SeasonRaceCreateRequest(
    val trackId: TrackId,
    val positions: List<ParticipantId>,
    val laps: Int,
)
