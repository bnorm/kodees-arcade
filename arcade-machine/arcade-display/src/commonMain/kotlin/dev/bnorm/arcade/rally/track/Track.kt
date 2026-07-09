package dev.bnorm.arcade.rally.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.Density
import dev.bnorm.arcade.rally.Track

@Composable
fun Track(
    track: Track,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberTrackBitmap(track)
    Box(
        modifier = modifier
            .background(Color(red = 0x00, green = 0x55, blue = 0x00))
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
fun rememberTrackBitmap(
    track: Track,
): ImageBitmap {
    return remember(track) {
        renderComposeScene(
            width = track.width.toInt(),
            height = track.height.toInt(),
            density = Density(1f),
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawTrack(
                    checkpoints = track.checkpoints,
                    startingLine = track.checkpoints.firstOrNull(),
                    positions = track.positions,
                    complete = true,
                )
            }
        }.toComposeImageBitmap()
    }
}
