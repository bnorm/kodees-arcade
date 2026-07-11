package dev.bnorm.arcade.worker

import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.race.WasmDriver
import dev.bnorm.arcade.rally.race.WasmGame
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.TrackResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import dev.bnorm.arcade.rally.Track as RallyTrack

class LocalGame(
    private val client: ArcadeClient,
    private val id: RaceId,
) : Game {
    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        val race = client.getRace(id) // TODO error
        val track = client.getTrack(race.trackId) // TODO error
        require(race.positions.size <= track.positions.size)

        val rallyDrivers = buildList {
            for (driver in race.positions) {
                val blob = client.downloadDriverVersion(driver.driverId, driver.version) // TODO error
                add(WasmDriver(driver.name, blob))
            }
        }

        val wasmRace = WasmGame(track.toRallyTrack(), rallyDrivers, race.laps)
        wasmRace.start(onEvent)
    }
}

private fun TrackResponse.toRallyTrack(): RallyTrack {
    return RallyTrack(
        width = width,
        height = height,
        checkpoints = checkpoints,
        positions = positions,
    )
}
