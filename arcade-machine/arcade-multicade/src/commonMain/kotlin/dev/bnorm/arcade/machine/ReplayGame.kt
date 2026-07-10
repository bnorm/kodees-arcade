package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.protobuf.ProtoBuf

class ReplayGame(
    private val path: PlatformFile
) : Game {
    override val events: ReceiveChannel<Game.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            val channel = path.readChannel()
            while (channel.awaitContent()) {
                val size = channel.readInt()
                events.send(ProtoBuf.decodeFromByteArray(Game.Event.serializer(), channel.readByteArray(size)))
            }
        } finally {
            events.close()
        }
    }
}

internal expect fun PlatformFile.readChannel(): ByteReadChannel
