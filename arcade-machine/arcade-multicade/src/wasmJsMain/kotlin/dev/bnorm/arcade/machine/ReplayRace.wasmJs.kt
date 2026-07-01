package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import js.iterable.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import web.encoding.TextDecoder
import web.file.File

actual fun PlatformFile.lineFlow(): Flow<String> = flow {
    @OptIn(ExperimentalWasmJsInterop::class) val webFile = (webFile as WebFile.FileWrapper).file.unsafeCast<File>()
    val decoder = TextDecoder()

    var remaining = ""
    webFile.stream().asFlow().collect {
        val piece = decoder.decode(it)
        val lines = piece.lines()
        for ((index, line) in lines.withIndex()) {
            when (index) {
                lines.lastIndex -> {
                    remaining += line
                }

                0 -> {
                    emit(remaining + line)
                    remaining = ""
                }

                else -> {
                    emit(line)
                }
            }
        }
    }
    if (remaining.isNotBlank()) {
        emit(remaining)
    }
}
