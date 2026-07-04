package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class DriverVersionResponse(
    val version: Version
)
