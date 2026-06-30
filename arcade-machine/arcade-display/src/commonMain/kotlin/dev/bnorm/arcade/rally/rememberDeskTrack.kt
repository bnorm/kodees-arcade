package dev.bnorm.arcade.rally

import androidx.compose.runtime.*
import dev.bnorm.arcade.arcade_display.generated.resources.Res

@Composable
fun rememberDeskTrack(): Track? {
    var track by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(Unit) {
        track = loadTrack(Res.readBytes("files/track.json").decodeToString())
    }
    return track
}
