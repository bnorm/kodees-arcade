package dev.bnorm.arcade.machine

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.serialization.protobuf.ProtoBuf

class WriteGame(
    private val game: Game,
    private val writeScope: WriteScope,
) : Game {
    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        writeScope.write { channel ->
            game.start { event ->
                val bytes = ProtoBuf.encodeToByteArray(Game.Event.serializer(), event)
                channel.writeInt(bytes.size)
                channel.writeByteArray(bytes)

                onEvent(event)
            }

            // TODO should we write a table of data at the end of the file to make lookup more efficient?
            //   - like a ZIP file Central Directory.
            //   - each event is written to the output like it is now.
            //   - the start byte offset of every event is tracked; but maybe only every 'N' Event.Update. (N=100?)
            //   - we write the event start byte offsets at the end of the output.
            //   - we write the start byte offset of the event start byte offsets at the end of the output.
            //  to look up an event:
            //   - read the last 8 bytes to find the offset of the start byte offsets.
            //   - read the start byte offsets until the desired event byte offset is found.
            //   - read the events until the desired event is found.
            //  this table can be built in memory while streaming the actual events to the file
            //  could be easily recreated if needed
            //   - may want a version byte somewhere?
            //   - the first byte of the file could be a version byte?
            //  how efficient is all of this in a file system?
            //  how efficient is all of this in a browser?
            //  why this is important:
            //   - simplifies streaming in the server.
            //     - no longer need to deserialize all the events to fined the completed event.
            //     - everything goes straight to the blob and we post process to pull the information we want.
            //     - and we can quickly reload the information as needed.
            //   - would allow loading a summary of some kind when selecting a race file.
        }
    }

    fun interface WriteScope {
        suspend fun write(writer: suspend (channel: ByteWriteChannel) -> Unit)
    }
}
