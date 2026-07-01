package dev.bnorm.arcade.machine

import io.github.vinceglb.filekit.PlatformFile
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun PlatformFile.lineFlow(): Flow<String> = flow {
    file.toPath().bufferedReader(
        options = arrayOf(
            StandardOpenOption.READ,
        )
    ).use { writer ->
        for (line in writer.lineSequence()) {
            emit(line)
        }
    }
}
