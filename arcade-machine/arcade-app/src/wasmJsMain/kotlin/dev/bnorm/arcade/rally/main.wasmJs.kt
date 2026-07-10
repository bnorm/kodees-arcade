@file:OptIn(ExperimentalMaterial3Api::class)

package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.machine.ReplayGame
import dev.bnorm.arcade.server.client.ArcadeClient
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("composeApp") {
        val scope = rememberCoroutineScope()
        val client = remember {
            val hostname = window.location.hostname
            val port = window.location.port.toIntOrNull() ?: 8080
            if (hostname == "localhost") {
                ArcadeClient(host = hostname, port = port)
            } else {
                null
            }
        }

        val trackViewModel = TrackViewModel(scope)
        val gameViewModel = GameViewModel(TrackViewModel.INITIAL_TRACK, scope)

        Column {
            val recordingPicker = rememberFilePickerLauncher(
                mode = FileKitMode.Single,
                type = FileKitType.File("race"),
            ) { file ->
                if (file != null) {
                    gameViewModel.new(ReplayGame(file))
                }
            }

            var showDownloader by remember { mutableStateOf(false) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { recordingPicker.launch() }) {
                    Text("Load Recording")
                }
                if (client != null) {
                    Button(onClick = { gameViewModel.clear(); showDownloader = true }) {
                        Text("Download")
                    }
                }
            }

            if (showDownloader && client != null) {
                Dialog(onDismissRequest = { showDownloader = false }) {
                    RaceDownloader(
                        client,
                        onStart = {
                            gameViewModel.new(it)
                            showDownloader = false
                        }
                    )
                }
            }

            RaceWizard(client, trackViewModel, onStart = { gameViewModel.new(it) })

            GameScreen(gameViewModel)
        }
    }
}
