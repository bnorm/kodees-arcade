package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RaceCreateRequest(
    val trackId: TrackId,
    val racers: List<RacerId>,
)
