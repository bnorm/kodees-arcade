package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.protobuf.ProtoBuf

class ReplayRace(
    private val path: PlatformFile
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            val channel = path.readChannel()
            while (channel.awaitContent()) {
                val size = channel.readInt()
                events.send(ProtoBuf.decodeFromByteArray(Race.Event.serializer(), channel.readByteArray(size)))
            }
        } finally {
            events.close()
        }
    }
}

internal expect fun PlatformFile.readChannel(): ByteReadChannel
