package dev.bnorm.arcade.service.api

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class RaceId(val uuid: Uuid) {
    companion object {
        fun generate(): RaceId = RaceId(Uuid.generateV7())
    }
}
