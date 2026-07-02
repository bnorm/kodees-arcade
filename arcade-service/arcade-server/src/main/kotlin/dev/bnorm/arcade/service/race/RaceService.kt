package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.logger
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class RaceService(
    private val races: RaceRepository,
    private val blobs: BlobRepository,
    private val clock: Clock = Clock.System,
) : Service {
    companion object {
        private val log = logger<RaceService>()
    }

    // TODO umm... this needs to be better
    private val channel = Channel<RaceProcessEvent>(1_000)

    override suspend fun initialize() {
        for (entity in races.getIncompleteRaces()) {
            if (entity.endTime == null) {
                resetRace(entity.id)
            }
        }
    }

    suspend fun getAllRaces(): List<RaceResponse> {
        return races.getRaces().map { it.toResponse() }
    }

    suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        val entity = races.createRace(request.trackId, request.racerIds)
        submitRaceForProcessing(entity)
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
        if (!races.startRace(id, nonce, startTime = clock.now())) return null

        val blob = try {
            blobs.upload(channel)
        } catch (t: Throwable) {
            log.warn("error uploading race results", t)
            resetRace(id)
            throw t
        }

        try {
            if (!races.finishRace(id, nonce, endTime = clock.now(), blob.id)) TODO("should be impossible")
        } catch (t: Throwable) {
            log.warn("error finishing race", t)
            // TODO delete blob
            throw t
        }

        return getRace(id)
    }

    suspend fun resetRace(id: RaceId): RaceResponse? {
        val entity = races.resetRace(id) ?: return null
        submitRaceForProcessing(entity)
        return entity.toResponse()
    }

    private fun submitRaceForProcessing(entity: RaceEntity) {
        channel.trySend(RaceProcessEvent(entity.id, entity.nonce))
            .onClosed { TODO("should be impossible") }
            .onFailure { log.warn("large event processing backlog, could not reprocess race", it) }
    }

    fun listen(): Flow<RaceProcessEvent> = flow {
        for (event in channel) {
            try {
                emit(event)
            } catch (t: Throwable) {
                log.warn("error processing race event", t)
                resetRace(event.id)
                throw t
            }
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
