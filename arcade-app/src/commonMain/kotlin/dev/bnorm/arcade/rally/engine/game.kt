package dev.bnorm.arcade.rally.engine

import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.rally.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

expect fun CoroutineScope.game(
    track: Track,
    racers: List<Racer>,
    carImages: List<ImageBitmap>,
): ReceiveChannel<RallyGameState>
