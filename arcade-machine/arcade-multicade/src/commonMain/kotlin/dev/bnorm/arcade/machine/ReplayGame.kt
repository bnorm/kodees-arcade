package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import kotlinx.serialization.protobuf.ProtoBuf

class ReplayGame(
    private val path: PlatformFile
) : Game {
    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        val channel = path.readChannel()
        while (channel.awaitContent()) {
            val size = channel.readInt()
            onEvent(ProtoBuf.decodeFromByteArray(Game.Event.serializer(), channel.readByteArray(size)))
        }
    }
}

internal expect fun PlatformFile.readChannel(): ByteReadChannel
