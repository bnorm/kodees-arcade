package dev.bnorm.arcade.service

import dev.bnorm.arcade.rally.race.WasmRace
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.bnorm.arcade.service.repo.RaceRepository
import dev.bnorm.arcade.service.repo.RacerRepository
import dev.bnorm.arcade.service.repo.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import io.ktor.utils.io.writeStringUtf8
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import dev.bnorm.arcade.rally.Track as RallyTrack
import dev.bnorm.arcade.rally.race.Race as RallyRace
import dev.bnorm.arcade.rally.race.Racer as RallyRacer

@SingleIn(AppScope::class)
@Inject
class RaceRunner(
    val blobs: BlobRepository,
    val tracks: TrackRepository,
    val racers: RacerRepository,
    val races: RaceRepository,
    val clock: Clock = Clock.System
) {
    companion object {
        private val log = logger<RaceRunner>()
    }

    suspend fun start(id: RaceId, consumer: SendChannel<String>? = null): RaceEntity? {
        val race = races.getRace(id) ?: return null // TODO error?
        val track = tracks.getTrack(race.trackId) ?: return null
        val trackBlob = blobs.download(track.blobId) ?: return null

        val rallyTrack = Json.decodeFromString(RallyTrack.serializer(), trackBlob.toByteArray().decodeToString())
        val rallyRacers = buildList {
            for (id in race.racers) {
                val racer = racers.getRacer(id) ?: return null
                val (_, blobId) = racer.versions.lastEntry() ?: return null
                val blob = blobs.download(blobId) ?: return null
                add(RallyRacer(racer.name, blob.toByteArray()))
            }
        }

        return coroutineScope {
            races.startRace(id, startTime = clock.now())
            log.info("Starting race: $id")

            val channel = ByteChannel()
            val deferredBlobId = async { blobs.upload(channel).id }

            val rallyRace = WasmRace(rallyTrack, rallyRacers)
            launch { rallyRace.start() }
            writeEvents(rallyRace, channel, consumer)

            deferredBlobId.await().let { blobId ->
                log.info("Finished race: $id")
                races.finishRace(id, endTime = clock.now(), blobId)
            }
        }
    }

    private suspend fun writeEvents(
        rallyRace: WasmRace,
        channel: ByteChannel,
        consumer: SendChannel<String>?
    ) {
        try {
            channel.use {
                var clearableConsumer = consumer
                for (event in rallyRace.events) {
                    val json = Json.encodeToString(RallyRace.Event.serializer(), event)
                    channel.writeStringUtf8(json)
                    channel.writeStringUtf8("\n")
                    try {
                        clearableConsumer?.send(json)
                    } catch (e: Throwable) {
                        // Clear the consumer to not repeat exception.
                        clearableConsumer = null
                        if (e !is CancellationException) {
                            // CancellationException is normal when consumer cancels channel.
                            // Anything else is abnormal so log it.
                            log.warn("error consuming race events", e)
                        }
                    }
                }
            }
        } finally {
            consumer?.close()
        }
    }
}
