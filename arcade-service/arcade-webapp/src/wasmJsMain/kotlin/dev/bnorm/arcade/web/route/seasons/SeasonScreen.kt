package dev.bnorm.arcade.web.route.seasons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.internal.Grid
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.ParticipantResponse
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.bnorm.arcade.web.route.races.RaceCard
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.roundToInt

@Inject
@SingleIn(AppScope::class)
class SeasonScreen(
    private val client: ArcadeClient,
) {
    @Composable
    fun Content(seasonId: SeasonId) {
        var season by remember { mutableStateOf<SeasonResponse?>(null) }
        val participants = remember { mutableStateListOf<ParticipantResponse>() }
        val races = remember { mutableStateListOf<RaceResponse>() }
        LaunchedEffect(seasonId) {
            season = client.getSeason(seasonId)
            participants.addAll(client.getParticipants(seasonId))
            races.addAll(client.getSeasonRaces(seasonId))
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${season?.name}", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                // RaceCreateButton(client, onCreate = { races.add(it) })
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(top = 16.dp)
                ) {
                    for (race in races) {
                        RaceCard(client, race)
                    }
                }

                Grid(
                    rows = 1 + participants.size,
                    stickyRows = 1,
                    columns = 2,
                    modifier = Modifier
                        .padding(32.dp - 4.dp)
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(4.dp)
                    )
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(4.dp)
                    )
                    for (participant in participants.sortedBy { -it.score }) {
                        Text(
                            text = "${participant.name} ${participant.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                        Text(
                            text = "${participant.score.roundToInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
