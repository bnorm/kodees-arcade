package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import kotlinx.serialization.protobuf.ProtoBuf

class DownloadGame(
    private val client: ArcadeClient,
    private val raceId: RaceId,
) : Game {
    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        client.downloadRace(raceId).collect { line ->
            onEvent(ProtoBuf.decodeFromByteArray(Game.Event.serializer(), line))
        }
    }
}
