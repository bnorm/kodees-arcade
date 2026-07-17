package dev.bnorm.arcade.web.route.seasons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.internal.Grid
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.ParticipantResponse
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.SeasonKey
import dev.bnorm.arcade.web.components.MaxWidthContent
import dev.bnorm.arcade.web.route.RouteScreen
import dev.bnorm.arcade.web.route.races.RaceCard
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.math.roundToInt
import kotlin.reflect.KClass

@ContributesIntoSet(AppScope::class, binding<RouteScreen<RouteKey>>())
class SeasonScreen(
    private val client: ArcadeClient,
) : RouteScreen<SeasonKey> {
    override val key: KClass<out SeasonKey>
        get() = SeasonKey::class

    @Composable
    override fun Content(key: SeasonKey) {
        Content(key.id)
    }

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

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {

                    },
                    title = {
                        Text("${season?.name}")
                    },
                    actions = {

                    }
                )
            }
        ) { contentPadding ->
            MaxWidthContent(
                modifier = Modifier
                    .padding(contentPadding)
            ) {
                SeasonContent(races, participants)
            }
        }
    }

    @Composable
    private fun SeasonContent(
        races: SnapshotStateList<RaceResponse>,
        participants: SnapshotStateList<ParticipantResponse>
    ) {
        Column {
            var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedDestination,
                divider = {},
                modifier = Modifier
            ) {
                Tab(
                    selected = selectedDestination == 0,
                    onClick = { selectedDestination = 0 },
                    text = { Text("Participants") },
                )
                Tab(
                    selected = selectedDestination == 1,
                    onClick = { selectedDestination = 1 },
                    text = { Text("Races") },
                )
            }

            if (selectedDestination == 1) {
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
            }

            if (selectedDestination == 0) {
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
