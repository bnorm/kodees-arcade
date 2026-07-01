package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.bnorm.arcade.service.repo.RaceRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class RaceService(
    private val races: RaceRepository,
    private val blobs: BlobRepository,
    private val clock: Clock = Clock.System,
) : Service {
    // TODO umm... this needs to be better
    private val channel = Channel<RaceProcessEvent>(1_000)

    override suspend fun initialize() {
        for (entity in races.getRaces()) {
            if (entity.endTime == null) {
                channel.send(RaceProcessEvent(entity.id, entity.nonce))
            }
        }
    }

    suspend fun getAllRaces(): List<RaceResponse> {
        return races.getRaces().map { it.toResponse() }
    }

    suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        val entity = races.createRace(request.trackId, request.racerIds)
        channel.send(RaceProcessEvent(entity.id, entity.nonce))
        return entity.toResponse()
    }

    suspend fun getRace(id: RaceId): RaceResponse? {
        return races.getRace(id)?.toResponse()
    }

    suspend fun downloadRace(id: RaceId): Pair<RaceResponse?, ByteReadChannel?> {
        // TODO yuk response... sealed class? special exceptions for HTTP responses?
        val entity = races.getRace(id) ?: return null to null
        if (entity.blobId != null) {
            val download = blobs.download(entity.blobId) ?: TODO("should be impossible")
            return entity.toResponse() to download
        } else {
            return entity.toResponse() to null
        }
    }

    suspend fun uploadRace(id: RaceId, nonce: Nonce, channel: ByteReadChannel): RaceResponse? {
        val entity = races.getRace(id) ?: return null

        val startTime = clock.now()
        if (!races.startRace(id, nonce, startTime)) return null

        val blob = try {
            blobs.upload(channel)
        } catch (t: Throwable) {
            // TODO reset nonce and resubmit to channel
            t.printStackTrace()
            throw t
        }

        val endTime = clock.now()
        if (!races.finishRace(id, nonce, endTime, blob.id)) TODO("should be impossible")

        return entity.copy(
            startTime = startTime,
            endTime = endTime,
        ).toResponse()
    }

    fun listen(): Flow<RaceProcessEvent> = flow {
        for (event in channel) {
            emit(event)
        }
    }

    private fun RaceEntity.toResponse(): RaceResponse {
        return RaceResponse(
            id = this.id,
            trackId = this.trackId,
            startTime = this.startTime,
            endTime = this.endTime,
            racers = this.racers.toList(),
        )
    }
}
