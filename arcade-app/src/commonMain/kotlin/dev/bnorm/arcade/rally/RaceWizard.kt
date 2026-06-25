package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_samples.generated.resources.BundledRacers
import dev.bnorm.arcade.rally.race.ActiveRace
import dev.bnorm.arcade.rally.race.Race
import dev.bnorm.arcade.rally.race.Racer
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

private val BUNDLED_RACERS = listOf("Kodee", "Snail")

private class WasmRacer(
    override val name: String,
    override val bytes: ByteArray,
) : Racer

@Composable
fun RaceWizard(
    track: Track,
    onStart: (Race) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val racers = remember { mutableStateListOf<WasmRacer>() }

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
                    WasmRacer(
                        name = pickRacerName(file.name.substringBeforeLast(".")),
                        bytes = file.readBytes(),
                    )
                )
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
                Text("Pick Racer")
            }
            Button(
                enabled = racers.isNotEmpty(),
                onClick = {
                    racers.clear()
                }
            ) {
                Text("Clear")
            }
            Text("Quick Add: ")
            for (racer in BUNDLED_RACERS) {
                Button(
                    enabled = canAddRacer(),
                    onClick = {
                        scope.launch {
                            racers.add(
                                WasmRacer(
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
                onStart(ActiveRace(track, racers.toList()))
            }
        ) {
            Text("Start!")
        }
    }
}
