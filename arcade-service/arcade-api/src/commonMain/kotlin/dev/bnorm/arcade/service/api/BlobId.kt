package dev.bnorm.arcade.service.api

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class BlobId(val uuid: Uuid) {
    companion object {
        fun generate(): BlobId = BlobId(Uuid.generateV7())
    }
}
