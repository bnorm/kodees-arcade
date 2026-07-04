package dev.bnorm.arcade.worker

import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.rally.race.WasmDriver
import dev.bnorm.arcade.rally.race.WasmRace
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import dev.bnorm.arcade.rally.Track as RallyTrack

class LocalRace(
    private val client: ArcadeClient,
    private val id: RaceId,
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel()

    override suspend fun start() {
        try {
            val race = client.getRace(id) // TODO error
            val track = client.getTrack(race.trackId) // TODO error
            val trackBlob = client.downloadTrack(track.id) // TODO error

            val rallyTrack = Json.decodeFromString(RallyTrack.serializer(), trackBlob.decodeToString())
            val rallyDrivers = buildList {
                for (driver in race.positions) {
                    val blob = client.downloadDriverVersion(driver.driverId, driver.version) // TODO error
                    add(WasmDriver(driver.name, blob))
                }
            }

            coroutineScope {
                val wasmRace = WasmRace(rallyTrack, rallyDrivers)
                launch { wasmRace.start() }
                wasmRace.events.consumeEach {
                    events.send(it)
                }
            }
        } catch (e: Throwable) {
            events.close(e)
        } finally {
            events.close()
        }
    }
}
