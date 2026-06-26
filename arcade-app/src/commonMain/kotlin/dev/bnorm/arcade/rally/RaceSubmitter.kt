package dev.bnorm.arcade.rally

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.StreamRace
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import kotlinx.coroutines.async

@Composable
fun RaceSubmitter(
    client: ArcadeClient,
    onStart: (Race) -> Unit,
) {
    val tracks = remember { mutableStateListOf<TrackResponse>() }
    val racers = remember { mutableStateListOf<RacerResponse>() }

    LaunchedEffect(client) {
        val foundTracks = async { client.getTracks() }
        val foundRacers = client.getRacers()
        foundTracks.await() // Force wait.
        tracks.clear()
        tracks.addAll(foundTracks.await())
        racers.clear()
        racers.addAll(foundRacers)
    }

    var selectedTrack by remember { mutableStateOf<TrackId?>(null) }
    val selectedRacers = remember { mutableStateSetOf<RacerId>() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(Modifier.weight(1f)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Tracks", style = MaterialTheme.typography.headlineSmall)
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(tracks) { track ->
                        val isSelected = selectedTrack == track.id

                        Text(
                            text = track.id.uuid.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color.Gray else Color.White)
                                .clickable { selectedTrack = track.id }
                                .padding(4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Racers", style = MaterialTheme.typography.headlineSmall)
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(racers) { racer ->
                        val isSelected = selectedRacers.contains(racer.id)

                        Text(
                            text = racer.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color.Gray else Color.White)
                                .clickable {
                                    when (isSelected) {
                                        true -> selectedRacers.remove(racer.id)
                                        false -> selectedRacers.add(racer.id)
                                    }
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
        Button(
            enabled = selectedTrack != null && selectedRacers.isNotEmpty(),
            onClick = {
                val request = RaceCreateRequest(selectedTrack!!, selectedRacers.toList())
                onStart(StreamRace(client, request))
            }
        ) {
            Text("Submit!")
        }
    }
}
