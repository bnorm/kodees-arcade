package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

// TODO rename to driver?
@Serializable
class RacerVersionResponse(
    val version: Version
)
