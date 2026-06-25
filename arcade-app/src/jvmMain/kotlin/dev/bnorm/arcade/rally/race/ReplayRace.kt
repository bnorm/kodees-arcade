package dev.bnorm.arcade.rally.race

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader

class ReplayRace(
    val path: Path
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            path.bufferedReader(
                options = arrayOf(
                    StandardOpenOption.READ,
                )
            ).use { writer ->
                for (line in writer.lineSequence()) {
                    events.send(Json.decodeFromString(Race.Event.serializer(), line))
                }
            }

        } finally {
            events.close()
        }
    }
}
