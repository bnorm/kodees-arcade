@file:OptIn(ExperimentalMaterial3Api::class)

package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.machine.ReplayRace
import dev.bnorm.arcade.server.client.ArcadeClient
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("composeApp") {
        val client = remember {
            val hostname = window.location.hostname
            val port = window.location.port.toIntOrNull() ?: 8080
            if (hostname == "localhost") {
                ArcadeClient(host = hostname, port = port)
            } else {
                null
            }
        }

        val track = rememberDeskTrack()
        if (track != null) {
            Column {
                var race by remember { mutableStateOf<Race?>(null) }
                LaunchedEffect(race) {
                    race?.start()
                }

                val recordingPicker = rememberFilePickerLauncher(
                    mode = FileKitMode.Single,
                    type = FileKitType.File("race"),
                ) { file ->
                    if (file != null) {
                        race = ReplayRace(file)
                    }
                }

                var showSubmitter by remember { mutableStateOf(false) }
                var showDownloader by remember { mutableStateOf(false) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { recordingPicker.launch() }) {
                        Text("Load Recording")
                    }
                    if (client != null) {
                        Button(onClick = { race = null; showSubmitter = true }) {
                            Text("Submit")
                        }
                        Button(onClick = { race = null; showDownloader = true }) {
                            Text("Download")
                        }
                    }
                }

                if (showSubmitter && client != null) {
                    Dialog(onDismissRequest = { showSubmitter = false }) {
                        RaceSubmitter(
                            client,
                            onStart = {
                                race = it
                                showSubmitter = false
                            }
                        )
                    }
                }

                if (showDownloader && client != null) {
                    Dialog(onDismissRequest = { showDownloader = false }) {
                        RaceDownloader(
                            client,
                            onStart = {
                                race = it
                                showDownloader = false
                            }
                        )
                    }
                }

                RaceWizard(client, track, onStart = { race = it })

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
