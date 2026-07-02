package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel

internal actual fun PlatformFile.readChannel(): ByteReadChannel {
    return file.toPath().readChannel()
}
