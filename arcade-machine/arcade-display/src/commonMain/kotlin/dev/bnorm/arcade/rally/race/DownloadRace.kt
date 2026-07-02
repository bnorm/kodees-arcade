package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.protobuf.ProtoBuf

class DownloadRace(
    private val client: ArcadeClient,
    private val raceId: RaceId,
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(1_000)

    override suspend fun start() {
        try {
            client.downloadRace(raceId).collect { line ->
                events.send(ProtoBuf.decodeFromByteArray(Race.Event.serializer(), line))
            }
        } finally {
            events.close()
        }
    }
}
