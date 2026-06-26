package dev.bnorm.arcade.server.rally

import dev.bnorm.arcade.rally.BlobId
import dev.bnorm.arcade.rally.BlobRepository
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class RaceId(val uuid: Uuid) {
    companion object {
        fun generate(): RaceId = RaceId(Uuid.generateV7())
    }
}

@Serializable
class Race(
    val id: RaceId,
    val trackId: TrackId,
    val racers: List<RacerId>,
    val blobId: BlobId? = null,
)

class RaceRepository(
    private val blobs: BlobRepository,
) {
    private val races = mutableMapOf<RaceId, Race>()

    fun getRaces(): List<Race> {
        return races.values.toList()
    }

    fun createRace(trackId: TrackId, racers: List<RacerId>): Race {
        val id = RaceId.generate()
        val race = Race(id, trackId, racers)
        races[id] = race
        return race
    }

    suspend fun finishRace(id: RaceId, channel: ByteReadChannel): Race? {
        val oldRace = races[id] ?: return null

        val blobId = blobs.upload(channel)
        val newRace = Race(oldRace.id, oldRace.trackId, oldRace.racers, blobId)
        races[id] = newRace
        return newRace
    }

    fun getRace(id: RaceId): Race? {
        return races[id]
    }
}
