package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RaceProcessEvent(
    val id: RaceId,
    val nonce: Nonce,
)
