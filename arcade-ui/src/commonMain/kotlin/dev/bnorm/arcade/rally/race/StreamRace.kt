package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceCreateRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.json.Json

class StreamRace(
    private val client: ArcadeClient,
    private val request: RaceCreateRequest,
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(1_000)

    override suspend fun start() {
        try {
            client.streamRace(request).drop(1).collect { line ->
                events.send(Json.decodeFromString(Race.Event.serializer(), line))
            }
        } finally {
            events.close()
        }
    }
}
