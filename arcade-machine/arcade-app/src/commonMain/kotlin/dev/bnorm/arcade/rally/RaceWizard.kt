package dev.bnorm.arcade.rally

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.arcade_player_samples.generated.resources.BundledDrivers
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.race.WasmGame
import dev.bnorm.arcade.rally.race.WasmDriver
import dev.bnorm.arcade.display.track.TrackImage
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.Version
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

private val BUNDLED_DRIVERS = listOf("Kodee", "Snail")

@Composable
fun RaceWizard(
    client: ArcadeClient?,
    trackViewModel: TrackViewModel,
    onStart: (Game) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val selectedTrack = remember { mutableStateOf<Track?>(null) }

    val lapsTextState = rememberTextFieldState("25")
    val laps = lapsTextState.text.toString().toIntOrNull()

    val drivers = remember { mutableStateListOf<WasmDriver>() }

    fun pickDriverName(baseName: String): String {
        val existingNames = drivers.mapTo(mutableSetOf()) { it.name }
        var name = baseName
        if (name in existingNames) name = "$name (1)"
        var i = 1
        while (name in existingNames) {
            name = name.substringBeforeLast(" ") + " (${i++})"
        }
        return name
    }

    fun canAddDriver(): Boolean {
        return selectedTrack.value != null && drivers.size < selectedTrack.value!!.positions.size
    }

    val driversLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File("wasm"),
//        directory = PlatformFile("../arcade-player-samples/build/drivers/files"),
    ) { file ->
        if (file != null) {
            scope.launch {
                drivers.add(
                    WasmDriver(
                        name = pickDriverName(file.name.substringBeforeLast(".")),
                        bytes = file.readBytes(),
                    )
                )
            }
        }
    }

    class DriverDisplay(
        val id: DriverId,
        val version: Version,
        val name: String,
    )

    var showDownloader by remember { mutableStateOf(false) }
    val serverDrivers = remember { mutableStateListOf<DriverDisplay>() }
    if (client != null && showDownloader) {
        LaunchedEffect(Unit) {
            val foundDrivers = client.getDrivers()
            val foundVersions = foundDrivers
                .flatMap { driver ->
                    client.getDriverVersions(driver.id)
                        .map { DriverDisplay(driver.id, it.version, driver.name) }
                }
            serverDrivers.clear()
            serverDrivers.addAll(foundVersions)
        }

        Dialog(onDismissRequest = { showDownloader = false }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Drivers", style = MaterialTheme.typography.headlineSmall)
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(serverDrivers) { driver ->
                            val version = driver.version
                            val name = "${driver.name} $version"
                            Text(
                                text = name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val wasm = client.downloadDriverVersion(driver.id, version)
                                            drivers.add(WasmDriver(pickDriverName(name), wasm))
                                            showDownloader = false
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        val model by trackViewModel.models.collectAsState()
        TrackSelector(model.tracks, selectedTrack)

        Spacer(Modifier.width(8.dp))

        Text("Laps:", style = MaterialTheme.typography.titleLarge)
        TextField(
            state = lapsTextState,
            isError = laps == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = canAddDriver(),
                onClick = {
                    driversLauncher.launch()
                }
            ) {
                Text("Load Driver")
            }
            if (client != null) {
                Button(
                    enabled = canAddDriver(),
                    onClick = {
                        showDownloader = true
                    }
                ) {
                    Text("Download")
                }
            }
            Button(
                enabled = drivers.isNotEmpty(),
                onClick = {
                    drivers.clear()
                }
            ) {
                Text("Clear")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Add: ")
            for (driver in BUNDLED_DRIVERS) {
                Button(
                    enabled = canAddDriver(),
                    onClick = {
                        scope.launch {
                            drivers.add(
                                WasmDriver(
                                    name = pickDriverName(driver),
                                    bytes = BundledDrivers.readBytes("files/$driver.wasm"),
                                )
                            )
                        }
                    }
                ) {
                    Text(driver)
                }
            }
        }

        Text("Selected Drivers:", style = MaterialTheme.typography.titleLarge)

        for (driver in drivers) {
            Spacer(Modifier.width(8.dp))
            Text(driver.name)
        }

        Spacer(Modifier.weight(1f))

        Button(
            enabled = drivers.isNotEmpty() && selectedTrack.value != null && laps != null,
            onClick = {
                onStart(WasmGame(selectedTrack.value!!, drivers.toList(), laps!!))
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Start!")
        }
    }
}

@Composable
private fun TrackSelector(
    tracks: List<Track>,
    selectedTrack: MutableState<Track?>
) {
    val state = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
    ) {
        Text("Available Tracks:", style = MaterialTheme.typography.titleLarge)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state)
        ) {
            for (track in tracks) {
                key(track) {
                    val selected = selectedTrack.value == track
                    TrackImage(
                        track = track,
                        modifier = Modifier
                            .size(200.dp, 200.dp)
                            .border(
                                width = 4.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(6.dp)
                            .clickable { selectedTrack.value = track }
                    )
                }
            }
        }
    }
}
