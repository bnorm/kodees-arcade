package dev.bnorm.arcade.web.route.races

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun RaceSubmitter(
    client: ArcadeClient,
    onCreate: (RaceResponse) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tracks = remember { mutableStateListOf<TrackResponse>() }
    val racers = remember { mutableStateListOf<RacerResponse>() }

    LaunchedEffect(client) {
        val foundTracks = async { client.getTracks() }
        val foundRacers = client.getRacers().filter { it.versions.isNotEmpty() }
        foundTracks.await() // Force wait.
        tracks.clear()
        tracks.addAll(foundTracks.await())
        racers.clear()
        racers.addAll(foundRacers)
    }

    var selectedTrack by remember { mutableStateOf<TrackId?>(null) }
    val selectedRacers = remember { mutableStateSetOf<RacerId>() }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tracks",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(Modifier.width(2.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Racers",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 256.dp)
                        .padding(8.dp)
                ) {
                    Column {
                        for (track in tracks) {
                            val isSelected = selectedTrack == track.id

                            Text(
                                text = track.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color.Gray else Color.Transparent)
                                    .clickable { selectedTrack = track.id }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.Black)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 256.dp)
                        .padding(8.dp)
                ) {
                    Column {
                        for (racer in racers) {
                            val isSelected = selectedRacers.contains(racer.id)

                            Text(
                                text = "${racer.name} ${racer.versions.last()}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color.Gray else Color.Transparent)
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
        }
        Button(
            enabled = selectedTrack != null && selectedRacers.isNotEmpty(),
            onClick = {
                scope.launch {
                    val request = RaceCreateRequest(selectedTrack!!, selectedRacers.toList())
                    onCreate(client.createRace(request))
                }
            }
        ) {
            Text("Submit!")
        }
    }
}
