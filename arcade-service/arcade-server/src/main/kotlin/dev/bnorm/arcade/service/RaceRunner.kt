package dev.bnorm.arcade.service

import dev.bnorm.arcade.rally.race.WasmRace
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.bnorm.arcade.service.repo.RaceRepository
import dev.bnorm.arcade.service.repo.RacerRepository
import dev.bnorm.arcade.service.repo.TrackRepository
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import dev.bnorm.arcade.rally.Track as RallyTrack
import dev.bnorm.arcade.rally.race.Race as RallyRace
import dev.bnorm.arcade.rally.race.Racer as RallyRacer

class RaceRunner(
    val tracks: TrackRepository,
    val races: RaceRepository,
    val racers: RacerRepository,
    val blobs: BlobRepository,
) {
    companion object {
        private val log = logger<RaceRunner>()
    }

    suspend fun start(id: RaceId): RaceEntity? {
        val race = races.getRace(id) ?: return null
        val track = tracks.getTrack(race.trackId) ?: return null
        val trackBlob = blobs.download(track.blobId) ?: return null

        val rallyTrack = Json.decodeFromString(RallyTrack.serializer(), trackBlob.toByteArray().decodeToString())
        val rallyRacers = buildList {
            for (id in race.racers) {
                val racer = racers.getRacer(id) ?: return null
                val racerBlob = blobs.download(racer.blobId) ?: return null
                add(RallyRacer(racer.name, racerBlob.toByteArray()))
            }
        }

        return coroutineScope {
            val channel = ByteChannel()
            val updated = async { races.finishRace(id, channel)!! }

            log.info("Starting race: $id")
            val rallyRace = WasmRace(rallyTrack, rallyRacers)
            launch { rallyRace.start() }
            writeEvents(rallyRace, channel)

            updated.await().also {
                log.info("Finished race: $id")
            }
        }
    }

    private suspend fun writeEvents(rallyRace: WasmRace, channel: ByteChannel) {
        channel.use {
            for (event in rallyRace.events) {
                channel.writeStringUtf8(Json.encodeToString(RallyRace.Event.serializer(), event))
                channel.writeStringUtf8("\n")
            }
        }
    }
}
