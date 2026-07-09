package dev.bnorm.arcade.rally.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.TrackResponse

@Composable
fun TrackDownloader(
    client: ArcadeClient,
    onDownload: (Track) -> Unit,
) {
    val tracks = remember { mutableStateListOf<TrackResponse>() }

    LaunchedEffect(client) {
        val foundTracks = client.getTracks()
        tracks.clear()
        tracks.addAll(foundTracks)
    }

    var selectedTrack by remember { mutableStateOf<TrackResponse?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(Modifier.weight(1f)) {
            items(tracks) { track ->
                val isSelected = selectedTrack == track

                Text(
                    text = buildString {
                        append(track.name)
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .background(if (isSelected) Color.Gray else Color.White)
                        .clickable { selectedTrack = track }
                )
            }
        }
        Button(
            enabled = selectedTrack != null,
            onClick = {
                onDownload(selectedTrack!!.toTrack())
            }
        ) {
            Text("Download!")
        }
    }
}

fun TrackResponse.toTrack(): Track {
    return Track(
        width = width,
        height = height,
        checkpoints = checkpoints,
        positions = positions,
        laps = 25,
    )
}
