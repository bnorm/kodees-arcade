package dev.bnorm.arcade.rally.engine

import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.rally.Track
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

actual fun CoroutineScope.game(
    track: Track,
    paths: Map<String, PlatformFile>,
    carImages: List<ImageBitmap>
): ReceiveChannel<RallyGameState> {
    TODO("Not yet implemented")
}
