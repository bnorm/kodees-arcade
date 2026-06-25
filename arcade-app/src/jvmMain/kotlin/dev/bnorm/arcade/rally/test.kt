@file:OptIn(ExperimentalComposeUiApi::class)

package dev.bnorm.arcade.rally

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.RecordRace
import dev.bnorm.arcade.rally.race.ReplayRace
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
        val track = rememberTrack()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp)
        ) {
            var race by remember { mutableStateOf<Race?>(null) }
            LaunchedEffect(race) {
                race?.let {
                    it.start()
                    race = null
                }
            }

            var showWizard by remember { mutableStateOf(false) }
            val recordingPicker = rememberFilePickerLauncher(
                mode = PickerMode.Single,
                type = PickerType.File(listOf("race")),
                initialDirectory = "../arcade-app",
            ) { file ->
                if (file != null) {
                    race = ReplayRace(file.file.toPath())
                }
            }

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

                RaceTrack(
                    track = track,
                    race = race,
                    onComplete = { race = null },
                    onStop = { race = null },
                )
            }
        }
    }
}

