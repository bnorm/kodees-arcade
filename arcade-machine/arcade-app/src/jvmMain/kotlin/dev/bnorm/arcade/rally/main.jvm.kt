@file:OptIn(ExperimentalMaterial3Api::class)

package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.RaceViewModel
import dev.bnorm.arcade.display.TrackViewModel
import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.machine.RecordRace
import dev.bnorm.arcade.machine.ReplayRace
import dev.bnorm.arcade.rally.track.TrackBuilder
import dev.bnorm.arcade.rally.track.TrackDownloader
import dev.bnorm.arcade.server.client.ArcadeClient
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import java.nio.file.Paths

// TODO support team racing?
//  could be cool for drivers to try and assist each other

// TODO support heats and seasons?
//  same Wasm instance for drivers through the heats or entire season
//  tracks change over the course of the season or repeat for heats

// TODO full F1 style season?
//  teams
//  qualifying
//  some sort of endurance aspect for the whole season

fun main() {
    application {
        val scope = rememberCoroutineScope()
        val client = remember { ArcadeClient() }

        val trackViewModel = TrackViewModel(scope)
        val raceViewModel = RaceViewModel(scope)

        Window(
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            MenuBar {
                Menu("Race") {
                    RaceWizardItem(client, trackViewModel, raceViewModel)
                    RaceLoadItem(this@Window, raceViewModel)
                    RaceDownloadItem(client, raceViewModel)
                }

                Menu("Track") {
                    TrackBuilderItem(trackViewModel)
                    TrackDownloadItem(client, trackViewModel)
                }
            }

            var complete by remember { mutableStateOf<Race.Event.Complete?>(null) }
            complete?.let {
                BasicAlertDialog(
                    onDismissRequest = { complete = null },
                ) {
                    Surface {
                        RaceResults(it)
                    }
                }
            }

            val trackModel by trackViewModel.models.collectAsState()
            val raceModel by raceViewModel.models.collectAsState()
            RaceTrack(
                track = trackModel.track,
                race = raceModel.race,
                onComplete = {
                    complete = it
                    raceViewModel.clear()
                },
                onStop = {
                    raceViewModel.clear()
                },
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun MenuScope.RaceWizardItem(
    client: ArcadeClient,
    trackViewModel: TrackViewModel,
    raceViewModel: RaceViewModel,
) {
    val state = rememberDialogState(
        size = DpSize(400.dp, 300.dp),
        position = WindowPosition.PlatformDefault,
    )

    var visible by remember { mutableStateOf(false) }
    if (visible) {
        DialogWindow(
            title = "Race Wizard",
            state = state,
            onCloseRequest = { visible = false }
        ) {
            val trackModel by trackViewModel.models.collectAsState()

            RaceWizard(
                client,
                trackModel.track,
                onStart = {
                    raceViewModel.new(RecordRace(it, Paths.get("./recording.race")))
                    visible = false
                }
            )
        }
    }

    Item(
        text = "New",
        onClick = {
            raceViewModel.clear()
            visible = true
        }
    )
}

@Composable
private fun MenuScope.RaceLoadItem(
    scope: WindowScope,
    raceViewModel: RaceViewModel
) {
    val recordingPicker = scope.rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File("race"),
    ) { file ->
        if (file != null) {
            raceViewModel.new(ReplayRace(file))
        }
    }

    Item(
        text = "Load",
        onClick = {
            raceViewModel.clear()
            recordingPicker.launch()
        }
    )
}

@Composable
private fun MenuScope.RaceDownloadItem(
    client: ArcadeClient,
    raceViewModel: RaceViewModel
) {
    val state = rememberDialogState(
        size = DpSize(400.dp, 300.dp),
        position = WindowPosition.PlatformDefault,
    )

    var visible by remember { mutableStateOf(false) }
    if (visible) {
        DialogWindow(
            title = "Download Race",
            state = state,
            onCloseRequest = { visible = false }
        ) {
            RaceDownloader(
                client,
                onStart = {
                    raceViewModel.new(it)
                    visible = false
                }
            )
        }
    }

    Item(
        text = "Download",
        onClick = {
            raceViewModel.clear()
            visible = true
        }
    )
}

@Composable
private fun MenuScope.TrackBuilderItem(
    trackViewModel: TrackViewModel,
) {
    val state = rememberDialogState(
        size = DpSize(800.dp, 800.dp),
        position = WindowPosition.PlatformDefault,
    )

    var visible by remember { mutableStateOf(false) }
    if (visible) {
        DialogWindow(
            title = "Track Builder",
            state = state,
            onCloseRequest = { visible = false }
        ) {
            TrackBuilder(
                size = IntSize(600, 600),
                onSave = {
                    trackViewModel.new(it)
                    visible = false
                }
            )
        }
    }

    Item(
        text = "Create",
        onClick = {
            visible = true
        }
    )
}

@Composable
private fun MenuScope.TrackDownloadItem(
    client: ArcadeClient,
    trackViewModel: TrackViewModel,
) {
    val state = rememberDialogState(
        size = DpSize(400.dp, 300.dp),
        position = WindowPosition.PlatformDefault,
    )

    var visible by remember { mutableStateOf(false) }
    if (visible) {
        DialogWindow(
            title = "Track Download",
            state = state,
            onCloseRequest = { visible = false }
        ) {
            TrackDownloader(
                client,
                onDownload = {
                    trackViewModel.new(it)
                    visible = false
                }
            )
        }
    }

    Item(
        text = "Download",
        onClick = {
            visible = true
        }
    )
}
