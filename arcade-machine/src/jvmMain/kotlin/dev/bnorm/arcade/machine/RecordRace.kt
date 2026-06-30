package dev.bnorm.arcade.machine

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedWriter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// TODO is there a way to create this with JS as well?
//  - filekit is great for desktop
//  - but the API is not great for dealing with large files...
class RecordRace(
    private val race: Race,
    private val path: Path,
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            coroutineScope {
                launch {
                    path.bufferedWriter(
                        options = arrayOf(
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                        )
                    ).use { writer ->
                        for (event in race.events) {
                            writer.appendLine(Json.encodeToString(Race.Event.serializer(), event))
                            events.send(event)
                        }
                    }
                }

                race.start()
            }
        } finally {
            events.close()
        }
    }
}
