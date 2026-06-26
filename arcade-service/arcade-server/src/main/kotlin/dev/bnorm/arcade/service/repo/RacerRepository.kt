package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.BlobId
import dev.bnorm.arcade.service.api.RacerId
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.Serializable

@Serializable
class RacerEntity(
    val id: RacerId,
    val name: String,
    val blobId: BlobId,
)

class RacerRepository(
    private val blobs: BlobRepository
) {
    private val racers = mutableMapOf<RacerId, RacerEntity>()

    suspend fun createRacer(name: String, channel: ByteReadChannel): RacerId {
        val blobId = blobs.upload(channel)

        val id = RacerId.generate()
        val racer = RacerEntity(id, name, blobId)
        racers[id] = racer
        return id
    }

    fun getRacer(id: RacerId): RacerEntity? {
        return racers[id]
    }
}
