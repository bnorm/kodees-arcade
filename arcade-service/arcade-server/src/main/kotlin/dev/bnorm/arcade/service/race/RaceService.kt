package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.machine.MemorizeGame
import dev.bnorm.arcade.machine.ReadGame
import dev.bnorm.arcade.machine.WriteGame
import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.ParticipantId
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
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
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class RaceService(
    private val seasons: SeasonRepository,
    private val races: RaceRepository,
    private val drivers: DriverRepository,
    private val blobs: BlobRepository,
    private val listeners: Set<RaceListener>,
    private val clock: Clock = Clock.System,
) : Service {
    companion object {
        private val log = logger<RaceService>()
    }

    suspend fun getAllRaces(): List<RaceResponse> {
        return races.getRaces().map { it.toResponse() }
    }

    suspend fun getSeasonRaces(seasonId: SeasonId): List<RaceResponse> {
        return races.getRaces(seasonId).map { it.toResponse() }
    }

    suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        val versionByDriverId = request.positions.associateBy { it.driverId }
        val driverVersionIds = drivers.getDriverVersions(request.positions.map { it.driverId })
            .mapNotNull { entity ->
                entity.id.takeIf { versionByDriverId[entity.driverId]?.version == entity.version }
            }
        if (driverVersionIds.size != request.positions.size) {
            TODO("bad request - an unknown driver or version")
        }

        val entity = races.createRace(seasonId = null, request.trackId, driverVersionIds, request.laps)
        for (listener in listeners) {
            listener.onRaceCreated(entity)
        }
        return entity.toResponse()
    }

    suspend fun createRace(seasonId: SeasonId, request: SeasonRaceCreateRequest): RaceResponse {
        require(request.positions.isNotEmpty()) { "no participants" }

        val allParticipants = seasons.getParticipants(seasonId)

        val unknownParticipants = mutableSetOf<ParticipantId>()
        val participants = buildList {
            for (participantId in request.positions) {
                val participant = allParticipants.find { it.id == participantId }
                if (participant != null) {
                    add(participant.driverVersionId)
                } else {
                    unknownParticipants.add(participantId)
                }
            }
        }

        require(unknownParticipants.isEmpty()) { "non-season participants not allowed" }
        val duplicateParticipants = request.positions.duplicates()
        require(duplicateParticipants.isEmpty()) { "duplicate participants not allowed" }

        // TODO check track positions vs participants size

        val entity = races.createRace(seasonId, request.trackId, participants, request.laps)
        for (listener in listeners) {
            listener.onRaceCreated(entity)
        }
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
        val entity = races.startRace(id, nonce, startTime = clock.now()) ?: return null
        val game = MemorizeGame(ReadGame { reader -> reader(channel) })

        val blob = try {
            coroutineScope {
                val buffer = ByteChannel()
                val writeGame = WriteGame(game) { writer -> buffer.use { writer(buffer) } }
                launch { writeGame.start { } }
                blobs.upload(buffer)
            }
        } catch (t: Throwable) {
            log.warn("error uploading race results", t)
            throw t
        }

        val endTime = clock.now()
        val results = buildMap {
            val driverVersionIds = entity.drivers.associate { it.name to it.driverVersionId }
            val results = game.complete.results
            val numberOfLaps = results.maxOf { it.value.laps.size }
            val order = results.entries
                .sortedBy { (_, value) -> value.laps.getOrNull(numberOfLaps - 1) ?: Long.MAX_VALUE }
                .map { (key, _) -> key }
            for ((place, name) in order.withIndex()) {
                put(driverVersionIds.getValue(name), place.toDouble())
            }
        }

        try {
            val result = races.finishRace(
                id = id,
                nonce = nonce,
                endTime = endTime,
                results = results,
                blobId = blob.id,
            )
            if (!result) {
                TODO("should be impossible")
            }
        } catch (t: Throwable) {
            log.warn("error finishing race", t)
            // TODO delete blob
            throw t
        }

        val completed = entity.copy(
            endTime = endTime,
            drivers = entity.drivers.map {
                it.copy(result = results[it.driverVersionId])
            },
        )

        for (listener in listeners) {
            listener.onRaceComplete(completed)
        }

        return completed.toResponse()
    }
}

fun RaceEntity.toResponse(): RaceResponse {
    return RaceResponse(
        id = this.id,
        trackId = this.trackId,
        laps = this.laps,
        drivers = this.drivers.map {
            RaceResponse.Driver(
                position = it.position,
                driverId = it.driverId,
                name = it.name,
                version = it.version,
                result = it.result,
            )
        },
        startTime = this.startTime,
        endTime = this.endTime,
    )
}

private fun <T> Iterable<T>.duplicates(): Set<T> {
    val upstream = this
    return buildSet {
        val seen = mutableSetOf<T>()
        for (value in upstream) {
            if (!seen.add(value)) add(value)
        }
    }
}
