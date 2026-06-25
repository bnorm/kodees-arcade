package dev.bnorm.arcade.rally.race

import io.github.vinceglb.filekit.core.PlatformFile
import js.iterable.asFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.json.Json
import web.encoding.TextDecoder
import web.file.File

class ReplayRace(
    val file: PlatformFile
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel(capacity = 1_000)

    override suspend fun start() {
        try {
            val decoder = TextDecoder()
            @OptIn(ExperimentalWasmJsInterop::class) val webFile = file.file.unsafeCast<File>()

            suspend fun handle(line: String) {
                events.send(Json.decodeFromString(Race.Event.serializer(), line))
            }

            var remaining = ""
            webFile.stream().asFlow().collect {
                val piece = decoder.decode(it)
                val lines = piece.lines()
                for ((index, line) in lines.withIndex()) {
                    if (index == 0) handle(remaining + line)
                    else if (index == lines.lastIndex) remaining = line
                    else handle(line)
                }
            }
            if (remaining.isNotBlank()) {
                handle(remaining)
            }
        } finally {
            events.close()
        }
    }
}
