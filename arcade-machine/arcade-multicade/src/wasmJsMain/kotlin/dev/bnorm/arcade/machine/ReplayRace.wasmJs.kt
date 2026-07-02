package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writer
import js.iterable.iterator
import js.numbers.JsNumbers.toKotlinByte
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import web.file.File
import web.streams.read

internal actual fun PlatformFile.readChannel(): ByteReadChannel {
    return CoroutineScope(Dispatchers.Main).writer(CoroutineName("file-reader") + Dispatchers.Main, autoFlush = false) {
        @OptIn(ExperimentalWasmJsInterop::class) val webFile = (webFile as WebFile.FileWrapper).file.unsafeCast<File>()
        val reader = webFile.stream().getReader()
        while (true) {
            val read = reader.read()
            if (read.done) break
            val value = read.value!!
            val iterator = value.iterator()
            while (iterator.hasNext()) {
                channel.writeByte(iterator.next().toKotlinByte())
            }
        }
    }.channel
}
