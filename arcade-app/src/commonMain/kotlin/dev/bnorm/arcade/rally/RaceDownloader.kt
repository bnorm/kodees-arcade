package dev.bnorm.arcade.rally

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
import dev.bnorm.arcade.rally.race.DownloadRace
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId

private data class RaceDisplay(
    val id: RaceId,
    val racers: List<String>,
)

@Composable
fun RaceDownloader(
    client: ArcadeClient,
    onStart: (Race) -> Unit,
) {
    val races = remember { mutableStateListOf<RaceDisplay>() }

    LaunchedEffect(client) {
        val foundRaces = client.getRaces()
        val foundRacers = client.getRacers().associateBy { it.id }

        races.clear()
        races.addAll(foundRaces.map { race -> RaceDisplay(race.id, race.racers.map { foundRacers.getValue(it).name }) })
    }

    var selectedRace by remember { mutableStateOf<RaceId?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(Modifier.weight(1f)) {
            items(races) { race ->
                val isSelected = selectedRace == race.id

                Text(
                    text = buildString {
                        append(race.id.uuid)
                        append(" ")
                        append(race.racers)
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .background(if (isSelected) Color.Gray else Color.White)
                        .clickable { selectedRace = race.id }
                )
            }
        }
        Button(
            enabled = selectedRace != null,
            onClick = {
                onStart(DownloadRace(client, selectedRace!!))
            }
        ) {
            Text("Start!")
        }
    }
}
