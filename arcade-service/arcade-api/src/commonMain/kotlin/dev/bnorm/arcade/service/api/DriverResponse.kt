package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class DriverResponse(
    val id: DriverId,
    val name: String,
)
