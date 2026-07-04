package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class SeasonResponse(
    val id: SeasonId,
    val name: String,
)
