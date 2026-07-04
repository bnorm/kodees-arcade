package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class ParticipantResponse(
    val id: ParticipantId,
    val seasonId: SeasonId,
    val driverId: DriverId,
    val name: String,
    val version: Version,
)
