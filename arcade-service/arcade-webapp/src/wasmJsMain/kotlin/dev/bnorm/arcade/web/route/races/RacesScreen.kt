package dev.bnorm.arcade.web.route.races

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.display.asset.icon.play_arrow
import dev.bnorm.arcade.display.asset.icon.replay
import dev.bnorm.arcade.display.asset.icon.sports_motorsports
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.game.driver.DriverDebugViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.rally.race.DownloadGame
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.web.route.RacesKey
import dev.bnorm.arcade.web.route.RouteScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
class RacesScreen(
    private val client: ArcadeClient,
) : RouteScreen<RacesKey> {
    override val key: KClass<out RacesKey>
        get() = RacesKey::class

    private val races = mutableStateListOf<RaceModel>()

    @Composable
    override fun Content(key: RacesKey) {
        LaunchedEffect(Unit) {
            val tracks = client.getTracks().associateBy { it.id }
            races.addAll(client.getRaces().map { race ->
                race.toModel(tracks[race.trackId])
            })
        }

//        RaceCreateButton(
//            client,
//            onCreate = {
//                scope.launch {
//                    val track = client.getTrack(it.trackId)
//                    races.add(it.toModel(track))
//                }
//            }
//        )
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            for (race in races) {
                RaceCard(client, race)
            }
        }
    }
}

@Composable
private fun WatchRaceDialog(client: ArcadeClient, raceId: RaceId?, onDismiss: () -> Unit) {
    if (raceId == null) return

    Dialog(onDismissRequest = {
        onDismiss()
    }) {
        val scope = rememberCoroutineScope()
        val driverDebugViewModel = remember {
            DriverDebugViewModel(scope)
        }
        val gameViewModel = remember(driverDebugViewModel) {
            GameViewModel(TrackViewModel.INITIAL_TRACK, scope, driverDebugViewModel)
        }

        LaunchedEffect(Unit) {
            gameViewModel.new(DownloadGame(client, raceId))
        }

        Card {
            GameScreen(
                gameViewModel,
                driverDebugViewModel,
                showDebug = false,
                modifier = Modifier
                    .padding(16.dp)
                    .height(IntrinsicSize.Min)
                    .width(IntrinsicSize.Min)
            )
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

    Button(
        onClick = { displayDialog = true }
    ) {
        // TODO better icon
        Icon(sports_motorsports, contentDescription = "Submit race")
        Spacer(Modifier.width(4.dp))
        Text("Submit", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun RaceCard(
    client: ArcadeClient,
    race: RaceModel,
) {
    var watch by remember { mutableStateOf(false) }
    WatchRaceDialog(client, race.id.takeIf { watch }, onDismiss = { watch = false })

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

            Row {
                Text(race.track.name, style = MaterialTheme.typography.titleLarge)
                if (endTime != null) {
                    Spacer(Modifier.weight(1f).widthIn(min = 32.dp))
                    PlayRaceButton(onWatch = { watch = true })
                } else {
                    Spacer(Modifier.weight(1f).widthIn(min = 32.dp))
                    ResetRaceButton(client, race.id)
                }
            }

            // TODO show duration?
            if (endTime != null) {
                Row {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Text("Finished ")
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
                            Text("${(now - endTime).toAgoString()} ago.")
                        }
                    }
                }
            } else if (startTime != null) {
                Row {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Text("Started ")
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
                            Text("${(now - startTime).toAgoString()} ago.")
                        }
                    }
                }
            } else {
                Text(
                    text = "Not started yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column {
                for (driver in race.drivers.sortedBy { it.result ?: Double.MAX_VALUE }) {
                    Row {
                        Text(
                            text = buildString {
                                val result = driver.result
                                if (result != null) {
                                    append(result.roundToInt() + 1)
                                    append(". ")
                                } else if (endTime != null) {
                                    append("DNF. ")
                                }
                                append(driver.name)
                                append(" ")
                                append(driver.version)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayRaceButton(onWatch: () -> Unit) {
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

@Composable
private fun ResetRaceButton(
    client: ArcadeClient,
    id: RaceId,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                indication = ripple(bounded = false, radius = 24.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                scope.launch {
                    client.resetRace(id)
                }
            }
    ) {
        // TODO this doesn't look truly centered...
        Icon(replay, contentDescription = "Reset race")
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
