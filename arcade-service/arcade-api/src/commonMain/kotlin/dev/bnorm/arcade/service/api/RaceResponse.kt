package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RaceResponse(
    val id: RaceId,
    val trackId: TrackId,
    val racers: List<RacerId>,
    val blobId: BlobId? = null,
)
