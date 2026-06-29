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
import dev.bnorm.arcade.arcade_samples.generated.resources.BundledRacers
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.Racer
import dev.bnorm.arcade.rally.race.WasmRace
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RacerResponse
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

private val BUNDLED_RACERS = listOf("Kodee", "Snail")

@Composable
fun RaceWizard(
    client: ArcadeClient?,
    track: Track,
    onStart: (Race) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val racers = remember { mutableStateListOf<Racer>() }

    fun pickRacerName(baseName: String): String {
        val existingNames = racers.mapTo(mutableSetOf()) { it.name }
        var name = baseName
        if (name in existingNames) name = "$name (1)"
        var i = 1
        while (name in existingNames) {
            name = name.substringBeforeLast(" ") + " (${i++})"
        }
        return name
    }

    fun canAddRacer(): Boolean = racers.size < 6

    val racersLauncher = rememberFilePickerLauncher(
        mode = PickerMode.Single,
        type = PickerType.File(listOf("wasm")),
        initialDirectory = "../arcade-samples/build/racers/files",
    ) { file ->
        if (file != null) {
            scope.launch {
                racers.add(
                    Racer(
                        name = pickRacerName(file.name.substringBeforeLast(".")),
                        bytes = file.readBytes(),
                    )
                )
            }
        }
    }

    var showDownloader by remember { mutableStateOf(false) }
    val serverRacers = remember { mutableStateListOf<RacerResponse>() }
    if (client != null && showDownloader) {
        LaunchedEffect(Unit) {
            val foundRacers = client.getRacers().filter { it.versions.isNotEmpty() }
            serverRacers.clear()
            serverRacers.addAll(foundRacers)
        }

        Dialog(onDismissRequest = { showDownloader = false }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Racers", style = MaterialTheme.typography.headlineSmall)
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(serverRacers) { racer ->
                            val version = racer.versions.last()
                            val name = "${racer.name} $version"
                            Text(
                                text = name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val wasm = client.downloadRacerVersion(racer.id, version)
                                            racers.add(Racer(pickRacerName(name), wasm))
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
                enabled = canAddRacer(),
                onClick = {
                    racersLauncher.launch()
                }
            ) {
                Text("Load Racer")
            }
            if (client != null) {
                Button(
                    enabled = canAddRacer(),
                    onClick = {
                        showDownloader = true
                    }
                ) {
                    Text("Download")
                }
            }
            Button(
                enabled = racers.isNotEmpty(),
                onClick = {
                    racers.clear()
                }
            ) {
                Text("Clear")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Add: ")
            for (racer in BUNDLED_RACERS) {
                Button(
                    enabled = canAddRacer(),
                    onClick = {
                        scope.launch {
                            racers.add(
                                Racer(
                                    name = pickRacerName(racer),
                                    bytes = BundledRacers.readBytes("files/$racer.wasm"),
                                )
                            )
                        }
                    }
                ) {
                    Text(racer)
                }
            }
        }

        for (racer in racers) {
            Spacer(Modifier.width(8.dp))
            Text(racer.name)
        }

        Spacer(Modifier.width(8.dp))
        Button(
            enabled = racers.isNotEmpty(),
            onClick = {
                onStart(WasmRace(track, racers.toList()))
            }
        ) {
            Text("Start!")
        }
    }
}
