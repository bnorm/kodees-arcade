package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class ParticipantCreateRequest(
    val driverId: DriverId,
    val version: Version,
)
