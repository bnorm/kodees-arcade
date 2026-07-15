package dev.bnorm.arcade.machine

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.serialization.protobuf.ProtoBuf

class WriteGame(
    private val game: Game,
    private val writeScope: WriteScope,
) : Game {
    override fun setDebug(driver: String, debug: Boolean) {
        game.setDebug(driver, debug)
    }

    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        writeScope.write { channel ->
            game.start { event ->
                val bytes = ProtoBuf.encodeToByteArray(Game.Event.serializer(), event)
                channel.writeInt(bytes.size)
                channel.writeByteArray(bytes)

                onEvent(event)
            }
        }
    }

    fun interface WriteScope {
        suspend fun write(writer: suspend (channel: ByteWriteChannel) -> Unit)
    }
}
