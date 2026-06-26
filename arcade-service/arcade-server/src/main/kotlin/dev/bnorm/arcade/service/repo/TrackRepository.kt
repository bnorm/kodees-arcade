package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.BlobId
import dev.bnorm.arcade.service.api.TrackId
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.serialization.Serializable

@Serializable
class TrackEntity(
    val id: TrackId,
    val blobId: BlobId,
)

class TrackRepository(
    private val blobs: BlobRepository,
) {
    private val tracks = mutableMapOf<TrackId, TrackEntity>()

    suspend fun createTrack(json: String): TrackId {
        val blobId = blobs.upload(json.byteInputStream().toByteReadChannel())

        val id = TrackId.generate()
        val race = TrackEntity(id, blobId)
        tracks[id] = race
        return id
    }

    fun getTrack(id: TrackId): TrackEntity? {
        return tracks[id]
    }
}
