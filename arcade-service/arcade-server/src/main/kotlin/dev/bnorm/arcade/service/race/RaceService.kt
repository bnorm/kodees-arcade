package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.SeasonRaceCreateRequest
import dev.bnorm.arcade.service.logger
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.DriverRepository
import dev.bnorm.arcade.service.season.SeasonRepository
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
    private val seasons: SeasonRepository,
    private val races: RaceRepository,
    private val drivers: DriverRepository,
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

    suspend fun getSeasonRaces(seasonId: SeasonId): List<RaceResponse> {
        return races.getRaces(seasonId).map { it.toResponse() }
    }

    suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        val versionByDriverId = request.drivers.associateBy { it.id }
        val driverVersionIds = drivers.getDriverVersions(request.drivers.map { it.id })
            .mapNotNull { entity ->
                entity.id.takeIf { versionByDriverId[entity.driverId]?.version == entity.version }
            }
        if (driverVersionIds.size != request.drivers.size) {
            TODO("bad request - an unknown driver or version")
        }

        val entity = races.createRace(request.trackId, driverVersionIds)
        submitRaceForProcessing(entity)
        return entity.toResponse()
    }

    suspend fun createRace(seasonId: SeasonId, request: SeasonRaceCreateRequest): RaceResponse? {
        val validParticipants = seasons.getParticipants(seasonId)
        if (validParticipants.isEmpty()) return null // TODO season doesn't exist or no participants and don't know which

        val actualParticipants = if (request.participantIds.isEmpty()) {
            validParticipants
        } else {
            val requestedParticipants = request.participantIds.toMutableSet()
            require(requestedParticipants.size == request.participantIds.size) { "duplicate participants not allowed" }
            validParticipants.filter { requestedParticipants.remove(it.id) }.also {
                require(requestedParticipants.isEmpty()) { "non-season participants not allowed" }
            }
        }

        val entity = races.createRace(request.trackId, actualParticipants.map { it.driverVersionId })
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
            drivers = this.versionedDrivers.map {
                RaceResponse.Driver(
                    id = it.driverId,
                    name = it.name,
                    version = it.version,
                )
            }
        )
    }
}
