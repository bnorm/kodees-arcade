package dev.bnorm.arcade.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.machine.ReplayGame
import dev.bnorm.arcade.rally.RaceDownloader
import dev.bnorm.arcade.rally.RaceWizard
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@SingleIn(AppScope::class)
@Inject
class WebMenu(
    private val client: ArcadeClient? = null,
    private val trackViewModel: TrackViewModel,
    private val gameViewModel: GameViewModel,
) {
    @Composable
    fun Content() {
        var showWizard by remember { mutableStateOf(false) }

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
            Button(onClick = { showWizard = true }) {
                Text("New Race")
            }
            Button(onClick = { recordingPicker.launch() }) {
                Text("Load Recording")
            }
            if (client != null) {
                Button(onClick = { gameViewModel.clear(); showDownloader = true }) {
                    Text("Download")
                }
            }
        }

        if (showWizard) {
            Dialog(
                onDismissRequest = { showWizard = false }
            ) {
                Surface(modifier = Modifier.height(IntrinsicSize.Min)) {
                    RaceWizard(
                        client,
                        trackViewModel,
                        onStart = {
                            gameViewModel.new(it)
                            showWizard = false
                        }
                    )
                }
            }
        }

        if (showDownloader && client != null) {
            Dialog(
                onDismissRequest = { showDownloader = false },
            ) {
                RaceDownloader(
                    client,
                    onStart = {
                        gameViewModel.new(it)
                        showDownloader = false
                    },
                    onError = {
                        showDownloader = false
                    }
                )
            }
        }
    }
}
