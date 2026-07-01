package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class ReplayRace(
    private val path: PlatformFile
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            path.lineFlow().collect { line ->
                events.send(Json.decodeFromString(Race.Event.serializer(), line))
            }
        } finally {
            events.close()
        }
    }
}

expect fun PlatformFile.lineFlow(): Flow<String>
