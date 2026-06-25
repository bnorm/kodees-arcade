package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.ReplayRace
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("composeApp") {
        val track = rememberTrack()
        if (track != null) {
            Column {
                var race by remember { mutableStateOf<Race?>(null) }
                LaunchedEffect(race) {
                    race?.let {
                        it.start()
                        race = null
                    }
                }

                val recordingPicker = rememberFilePickerLauncher(
                    mode = PickerMode.Single,
                    type = PickerType.File(listOf("race")),
                    initialDirectory = "../arcade-app",
                ) { file ->
                    if (file != null) {
                        race = ReplayRace(file)
                    }
                }

                Button(onClick = { recordingPicker.launch() }) {
                    Text("Load Recording")
                }
                RaceWizard(track, onStart = { race = it })
                RaceTrack(track, race, onComplete = { race = null }, onStop = { race = null })
            }
        }
    }
}
