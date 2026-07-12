package dev.bnorm.arcade.display.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.bnorm.arcade.display.internal.FixedSize
import dev.bnorm.arcade.driver.Track

@Composable
fun TrackImage(
    track: Track,
    modifier: Modifier = Modifier
) {
    FixedSize(
        size = IntSize(track.width.toInt(), track.height.toInt()),
        density = Density(1f),
        modifier = modifier
            .background(Color(red = 0x00, green = 0x55, blue = 0x00))
            .fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            drawTrack(
                checkpoints = track.checkpoints,
                startingLine = track.checkpoints.firstOrNull(),
                positions = track.positions,
                complete = true,
            )
        }
    }
}
