package dev.bnorm.arcade.rally

import androidx.compose.runtime.*
import dev.bnorm.arcade.arcade_app.generated.resources.Res

@Composable
fun rememberTrack(): Track? {
    var track by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(Unit) {
        track = loadTrack(Res.readBytes("files/track.json").decodeToString())
    }
    return track
}
