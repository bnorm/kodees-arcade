package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import kotlinx.serialization.protobuf.ProtoBuf

fun ReplayGame(path: PlatformFile): ReadGame {
    return ReadGame { reader ->
        reader(path.readChannel())
    }
}

class ReadGame(
    private val readScope: ReadScope,
) : Game {
    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        readScope.read { channel ->
            while (channel.awaitContent()) {
                val size = channel.readInt()
                onEvent(ProtoBuf.decodeFromByteArray(Game.Event.serializer(), channel.readByteArray(size)))
            }
        }
    }

    fun interface ReadScope {
        suspend fun read(reader: suspend (channel: ByteReadChannel) -> Unit)
    }
}

internal expect fun PlatformFile.readChannel(): ByteReadChannel
