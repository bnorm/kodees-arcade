package dev.bnorm.arcade.web.route.races

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.icons.play_arrow
import dev.bnorm.arcade.icons.sports_motorsports
import dev.bnorm.arcade.rally.RaceTrack
import dev.bnorm.arcade.rally.race.DownloadRace
import dev.bnorm.arcade.rally.rememberDeskTrack
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.web.route.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlin.time.Clock
import kotlin.time.Duration

@ContributesIntoSet(AppScope::class)
class RacesRoute(
    private val client: ArcadeClient
) : Route {
    override val path: String get() = "/races"

    @Composable
    override fun Content() {
        val races = remember { mutableStateListOf<RaceResponse>() }
        val racers = remember { mutableStateMapOf<RacerId, RacerResponse>() }
        val tracks = remember { mutableStateMapOf<TrackId, TrackResponse>() }
        LaunchedEffect(Unit) {
            val apiRacers = client.getRacers().associateBy { it.id }
            val apiTracks = client.getTracks().associateBy { it.id }
            val apiRaces = client.getRaces()
            races.clear()
            races.addAll(apiRaces)
            racers.clear()
            racers.putAll(apiRacers)
            tracks.clear()
            tracks.putAll(apiTracks)
        }

        var watchRaceId by remember { mutableStateOf<RaceId?>(null) }
        WatchRaceDialog(client, watchRaceId, onDismiss = { watchRaceId = null })

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Races", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                RaceCreateButton(client, onCreate = { races.add(it) })
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .width(IntrinsicSize.Max)
            ) {
                for (race in races) {
                    RaceCard(race, racers, tracks, onWatch = {
                        watchRaceId = race.id
                    })
                }
            }
        }
    }
}

@Composable
private fun WatchRaceDialog(client: ArcadeClient, raceId: RaceId?, onDismiss: () -> Unit) {
    if (raceId != null) {
        val race = remember(raceId) { DownloadRace(client, raceId) }
        LaunchedEffect(race) {
            race.start()
        }

        Dialog(onDismissRequest = {
            onDismiss()
        }) {
            Card {
                // TODO download this from the server
                val track = rememberDeskTrack()
                if (track != null) {
                    RaceTrack(track, race, onComplete = {}, onStop = {}, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RaceCreateButton(client: ArcadeClient, onCreate: (RaceResponse) -> Unit) {
    var displayDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        Dialog(onDismissRequest = { displayDialog = false }) {
            Card {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    RaceSubmitter(client, onCreate = {
                        displayDialog = false
                        onCreate(it)
                    })
                }
            }
        }
    }

    TextButton(
        onClick = { displayDialog = true }
    ) {
        // TODO better icon
        Icon(sports_motorsports, contentDescription = "Submit race")
        Spacer(Modifier.width(4.dp))
        Text("Submit", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RaceCard(
    race: RaceResponse,
    racers: SnapshotStateMap<RacerId, RacerResponse>,
    tracks: SnapshotStateMap<TrackId, TrackResponse>,
    onWatch: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            val now = Clock.System.now()
            val startTime = race.startTime
            val endTime = race.endTime

            val track = tracks.getValue(race.trackId)
            Row {
                Text(track.name, style = MaterialTheme.typography.titleLarge)
                if (endTime != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false, radius = 24.dp),
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onWatch()
                            }
                    ) {
                        // TODO this doesn't look truly centered...
                        Icon(play_arrow, contentDescription = "Play race")
                    }
                }
            }

            // TODO show duration?
            if (endTime != null) {
                // TODO tooltip only on ago string
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    state = rememberTooltipState(isPersistent = true),
                    tooltip = {
                        PlainTooltip {
                            // TODO with timezone formatter
                            Text(text = endTime.toString())
                        }
                    },
                ) {
                    Text(
                        text = "Finished ${(now - endTime).toAgoString()} ago.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (startTime != null) {
                // TODO tooltip only on ago string
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    state = rememberTooltipState(isPersistent = true),
                    tooltip = {
                        PlainTooltip {
                            // TODO with timezone formatter
                            Text(text = startTime.toString())
                        }
                    },
                ) {
                    Text(
                        text = "Started ${(now - startTime).toAgoString()} ago.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = "Not started yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column {
                // TODO this should be the same as race results
                for (racerId in race.racers) {
                    Text(racers.getValue(racerId).name, style = MaterialTheme.typography.bodyLarge)
                    // TODO include racer version
                }
            }
        }
    }
}

private fun Duration.toAgoString(): String {
    val seconds = this.inWholeSeconds
    if (seconds < 5L) {
        return "a few seconds"
    } else if (seconds < 60L) {
        return "$seconds seconds"
    } else {
        val minutes = this.inWholeMinutes
        if (minutes == 1L) {
            return "$minutes minute"
        } else if (minutes < 60L) {
            return "$minutes minutes"
        } else {
            val hours = this.inWholeHours
            if (hours == 1L) {
                return "$hours hour"
            } else if (hours < 24L) {
                return "$hours hours"
            } else {
                val days = this.inWholeDays
                return if (days == 1L) {
                    "$days day"
                } else {
                    "$days days"
                }
            }
        }
    }
}
