package dev.bnorm.arcade.machine

import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import java.nio.file.Path
import kotlinx.serialization.protobuf.ProtoBuf

// TODO is there a way to create this with JS as well?
//  - filekit is great for desktop
//  - but the API is not great for dealing with large files...
class RecordGame(
    private val game: Game,
    private val path: Path,
) : Game {
    override fun setDebug(driver: String, debug: Boolean) {
        game.setDebug(driver, debug)
    }

    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        path.toFile().writeChannel().use {
            game.start { event ->
                val bytes = ProtoBuf.encodeToByteArray(Game.Event.serializer(), event)
                writeInt(bytes.size)
                writeByteArray(bytes)

                onEvent(event)
            }
        }
    }
}
