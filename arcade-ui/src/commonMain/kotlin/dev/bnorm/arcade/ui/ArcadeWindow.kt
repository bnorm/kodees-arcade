package dev.bnorm.arcade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.runner.ArcadeCartridge
import dev.bnorm.arcade.runner.ArcadeController
import dev.bnorm.arcade.runner.loadCartridge
import dev.bnorm.arcade.runner.run
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun ArcadeWindow() {
    Box(Modifier.fillMaxSize()) {
        var cartridge by remember { mutableStateOf<ArcadeCartridge?>(null) }
        val cartridgeLauncher = rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("jar")),
            initialDirectory = "../games/cybertanks/cybertanks-cartridge/build/libs",
        ) { file ->
            if (file != null) {
                val path = file.path!!
                cartridge = loadCartridge(path)
            }
        }

        val controllers = remember { mutableStateListOf<ArcadeController.Factory>() }
        val controllersLauncher = rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("jar")),
            initialDirectory = "../games/cybertanks/cybertanks-sample/build/libs",
        ) { file ->
            if (file != null) {
                val cartridge = cartridge ?: return@rememberFilePickerLauncher
                controllers.addAll(cartridge.loadControllers(file.path!!))
            }
        }

        LaunchedEffect(Unit) {
            // TODO temporary automatically load cybertanks to make testing faster.
            val cybertankCartridge =
                loadCartridge("../games/cybertanks/cybertanks-cartridge/build/libs/cybertanks-cartridge-cartridge.jar")!!
            val cybertankControllers =
                cybertankCartridge.loadControllers("../games/cybertanks/cybertanks-sample/build/libs/cybertanks-sample-jvm.jar")

            cartridge = cybertankCartridge
            controllers.addAll(cybertankControllers)
        }

        var running by remember { mutableStateOf(false) }

        Column {
            Button(onClick = {
                running = false
//                controllers.forEach { it.close() }
                controllers.clear()
                cartridge?.close()
                cartridge = null
            }) {
                Text("Clear")
            }

            Button(enabled = !running && cartridge == null, onClick = { cartridgeLauncher.launch() }) {
                Text("Pick Cartridge")
            }

            if (cartridge != null) {
                Button(enabled = !running && controllers.isEmpty(), onClick = { controllersLauncher.launch() }) {
                    Text("Pick Agent")
                }
            }

            if (controllers.isNotEmpty()) {
                Row {
                    Button(enabled = !running, onClick = { running = true }) {
                        Text("Start")
                    }

                    Button(enabled = running, onClick = { running = false }) {
                        Text("Stop")
                    }
                }
            }

            if (running) {
                GameView(cartridge!!, controllers)
            }
        }
    }
}

@Composable
fun GameView(cartridge: ArcadeCartridge, controllers: List<ArcadeController.Factory>) {
    val density = LocalDensity.current

    var desiredFps by remember { mutableIntStateOf(120) }
    val frameDelay by derivedStateOf { (1.0 / desiredFps).seconds }

    val engine =
        remember(cartridge, controllers) { cartridge.engineFactory.create(controllers.map { it.create().agent }) }
    val render = remember(cartridge) { cartridge.renderFactory?.create() }
    val engineState by remember(engine) {
        flow {
            val startTime = TimeSource.Monotonic.markNow()
            var targetTime = frameDelay

            emitAll(engine.run().map {
                // Control FPS by delaying until target time for next frame.
                val currentTime = startTime.elapsedNow()
                delay(targetTime - currentTime)
                targetTime += frameDelay

                it.serialize()
            })
        }
    }
        .collectAsState(null, Dispatchers.Default)

    Column(Modifier.fillMaxSize()) {
        engineState?.let { data ->
            Row {
                TextField(
                    value = desiredFps.toString(),
                    onValueChange = { input -> input.toIntOrNull()?.let { fps -> desiredFps = fps } },
                    label = { Text("Desired FPS") }
                )
            }
            if (render != null) {
                with(density) {
                    Canvas(Modifier.requiredSize(800.toDp(), 600.toDp()).border(1.0.dp, Color.Red)) {
                        with(render) { draw(data) }
                    }
                }
            }
        }
    }
}

