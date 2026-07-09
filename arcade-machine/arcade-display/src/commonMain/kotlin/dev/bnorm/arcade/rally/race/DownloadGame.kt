package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.protobuf.ProtoBuf

class DownloadGame(
    private val client: ArcadeClient,
    private val raceId: RaceId,
) : Game {
    override val events: ReceiveChannel<Game.Event>
        field = Channel(1_000)

    override suspend fun start() {
        try {
            client.downloadRace(raceId).collect { line ->
                events.send(ProtoBuf.decodeFromByteArray(Game.Event.serializer(), line))
            }
        } finally {
            events.close()
        }
    }
}
