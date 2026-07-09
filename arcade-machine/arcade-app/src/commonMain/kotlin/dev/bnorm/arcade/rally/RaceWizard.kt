package dev.bnorm.arcade.rally

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import dev.bnorm.arcade.arcade_player_samples.generated.resources.BundledDrivers
import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.rally.race.WasmRace
import dev.bnorm.arcade.rally.race.WasmDriver
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
    track: Track,
    onStart: (Race) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // TODO entered laps
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

    fun canAddDriver(): Boolean = drivers.size < track.positions.size

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

    Column {
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

        for (driver in drivers) {
            Spacer(Modifier.width(8.dp))
            Text(driver.name)
        }

        Spacer(Modifier.width(8.dp))
        Button(
            enabled = drivers.isNotEmpty(),
            onClick = {
                onStart(WasmRace(track, drivers.toList(), laps = 25))
            }
        ) {
            Text("Start!")
        }
    }
}
