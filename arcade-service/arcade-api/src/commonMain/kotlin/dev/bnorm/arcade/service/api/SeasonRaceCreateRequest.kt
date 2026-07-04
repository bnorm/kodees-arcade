package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class SeasonRaceCreateRequest(
    val trackId: TrackId,
    val participantIds: List<ParticipantId> = emptyList(),
)
