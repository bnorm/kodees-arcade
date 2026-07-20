package dev.bnorm.arcade.service.worker

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class WorkerId(val uuid: Uuid) {
    companion object {
        fun generate(): WorkerId = WorkerId(Uuid.generateV7())
    }

    override fun toString(): String {
        return uuid.toString()
    }
}
