package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RacerResponse(
    val id: RacerId,
    val name: String,
)
