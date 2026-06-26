package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.BlobId
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.TrackId
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.Serializable

@Serializable
class RaceEntity(
    val id: RaceId,
    val trackId: TrackId,
    val racers: List<RacerId>,
    val blobId: BlobId? = null,
)

class RaceRepository(
    private val blobs: BlobRepository,
) {
    private val races = mutableMapOf<RaceId, RaceEntity>()

    fun getRaces(): List<RaceEntity> {
        return races.values.toList()
    }

    fun createRace(trackId: TrackId, racers: List<RacerId>): RaceEntity {
        val id = RaceId.generate()
        val race = RaceEntity(id, trackId, racers)
        races[id] = race
        return race
    }

    suspend fun finishRace(id: RaceId, channel: ByteReadChannel): RaceEntity? {
        val oldRace = races[id] ?: return null

        val blobId = blobs.upload(channel)
        val newRace = RaceEntity(oldRace.id, oldRace.trackId, oldRace.racers, blobId)
        races[id] = newRace
        return newRace
    }

    fun getRace(id: RaceId): RaceEntity? {
        return races[id]
    }
}
