package dev.bnorm.arcade.service.api

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SeasonId(val uuid: Uuid) {
    companion object {
        fun generate(): SeasonId = SeasonId(Uuid.generateV7())
    }

    override fun toString(): String {
        return uuid.toString()
    }
}
