package dev.bnorm.arcade.rally.race

import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader

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
