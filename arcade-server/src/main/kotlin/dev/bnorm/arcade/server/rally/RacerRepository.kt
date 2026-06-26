package dev.bnorm.arcade.server.rally

import dev.bnorm.arcade.rally.BlobId
import dev.bnorm.arcade.rally.BlobRepository
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class RacerId(val uuid: Uuid) {
    companion object {
        fun generate(): RacerId = RacerId(Uuid.generateV7())
    }
}

@Serializable
class Racer(
    val id: RacerId,
    val name: String,
    val blobId: BlobId,
)

class RacerRepository(
    private val blobs: BlobRepository
) {
    private val racers = mutableMapOf<RacerId, Racer>()

    suspend fun createRacer(name: String, channel: ByteReadChannel): RacerId {
        val blobId = blobs.upload(channel)

        val id = RacerId.generate()
        val racer = Racer(id, name, blobId)
        racers[id] = racer
        return id
    }

    fun getRacer(id: RacerId): Racer? {
        return racers[id]
    }
}
