@file:OptIn(ExperimentalMaterial3Api::class)

package dev.bnorm.arcade.rally

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.RecordRace
import dev.bnorm.arcade.rally.race.ReplayRace
import dev.bnorm.arcade.server.client.ArcadeClient
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import java.nio.file.Paths

// !!!
// This whole file is me just messing around to get something working that I can test.
// !!!

// TODO support team racing?
//  could be cool for racers to try and assist each other

// TODO support heats and seasons?
//  same Wasm instance for racers through the heats or entire season
//  tracks change over the course of the season or repeat for heats

// TODO full F1 style season?
//  teams
//  qualifying
//  some sort of endurance aspect for the whole season

fun main() {
    application {
        val client = remember { ArcadeClient() }
        val track = rememberTrack()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp)
        ) {
            var race by remember { mutableStateOf<Race?>(null) }
            LaunchedEffect(race) {
                race?.start()
            }

            var showWizard by remember { mutableStateOf(false) }
            val recordingPicker = rememberFilePickerLauncher(
                mode = PickerMode.Single,
                type = PickerType.File(listOf("race")),
            ) { file ->
                if (file != null) {
                    race = ReplayRace(file)
                }
            }

            var showSubmitter by remember { mutableStateOf(false) }
            var showDownloader by remember { mutableStateOf(false) }

            MenuBar {
                Menu("Race") {
                    Item(
                        text = "New",
                        onClick = {
                            race = null
                            showWizard = true
                        }
                    )
                    Item(
                        text = "Load",
                        onClick = {
                            race = null
                            recordingPicker.launch()
                        }
                    )
                    Item(
                        text = "Submit",
                        onClick = {
                            race = null
                            showSubmitter = true
                        }
                    )
                    Item(
                        text = "Download",
                        onClick = {
                            race = null
                            showDownloader = true
                        }
                    )
                }
            }

            if (track != null) {
                if (showWizard) {
                    DialogWindow(onCloseRequest = { showWizard = false }) {
                        RaceWizard(
                            track,
                            onStart = {
                                race = RecordRace(it, Paths.get("./recording.race"))
                                showWizard = false
                            }
                        )
                    }
                }

                if (showSubmitter) {
                    DialogWindow(onCloseRequest = { showSubmitter = false }) {
                        RaceSubmitter(
                            client,
                            onStart = {
                                race = it
                                showSubmitter = false
                            }
                        )
                    }
                }

                if (showDownloader) {
                    DialogWindow(onCloseRequest = { showDownloader = false }) {
                        RaceDownloader(
                            client,
                            onStart = {
                                race = it
                                showDownloader = false
                            }
                        )
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

                RaceTrack(
                    track = track,
                    race = race,
                    onComplete = {
                        complete = it
                        race = null
                    },
                    onStop = {
                        race = null
                    },
                )
            }
        }
    }
}

