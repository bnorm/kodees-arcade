package dev.bnorm.arcade.server.rally

import dev.bnorm.arcade.rally.BlobId
import dev.bnorm.arcade.rally.BlobRepository
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class TrackId(val uuid: Uuid) {
    companion object {
        fun generate(): TrackId = TrackId(Uuid.generateV7())
    }
}

@Serializable
class Track(
    val id: TrackId,
    val blobId: BlobId,
)

class TrackRepository(
    private val blobs: BlobRepository,
) {
    private val tracks = mutableMapOf<TrackId, Track>()

    suspend fun createTrack(json: String): TrackId {
        val blobId = blobs.upload(json.byteInputStream().toByteReadChannel())

        val id = TrackId.generate()
        val race = Track(id, blobId)
        tracks[id] = race
        return id
    }

    fun getTrack(id: TrackId): Track? {
        return tracks[id]
    }
}
