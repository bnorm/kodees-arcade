package dev.bnorm.arcade.machine

import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import java.nio.file.Path
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf

// TODO is there a way to create this with JS as well?
//  - filekit is great for desktop
//  - but the API is not great for dealing with large files...
class RecordGame(
    private val game: Game,
    private val path: Path,
) : Game {
    override val events: ReceiveChannel<Game.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            coroutineScope {
                launch {
                    path.toFile().writeChannel().use {
                            for (event in game.events) {
                                val bytes = ProtoBuf.encodeToByteArray(Game.Event.serializer(), event)
                                writeInt(bytes.size)
                                writeByteArray(bytes)
                                events.send(event)
                            }
                    }
                }

                game.start()
            }
        } finally {
            events.close()
        }
    }
}
