package dev.bnorm.arcade.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.engine.EngineResult
import dev.bnorm.arcade.runner.ArcadeCartridge
import dev.bnorm.arcade.runner.ArcadeController
import dev.bnorm.arcade.runner.loadCartridge
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Image

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

        val controllers = remember { mutableStateListOf<ArcadeController>() }
        val controllersLauncher = rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("jar")),
            initialDirectory = "../games/cybertanks/cybertanks-sample/build/libs",
        ) { file ->
            if (file != null) {
                val cartridge = cartridge ?: return@rememberFilePickerLauncher
                controllers.addAll(
                    cartridge
                        .loadControllers(file.path!!)
                        .filter { cartridge.engineFactory.isSupported(it.agent) }
                )
            }
        }

        var running by remember { mutableStateOf(false) }

        Column {
            Button(onClick = {
                running = false
                controllers.forEach { it.close() }
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
                Button(enabled = !running, onClick = { running = true }) {
                    Text("Launch")
                }
            }

            if (running) {
                GameView(cartridge!!, controllers)
            }
        }
    }
}

@Composable
fun GameView(cartridge: ArcadeCartridge, controllers: List<ArcadeController>) {
    val engine = remember(cartridge, controllers) { cartridge.engineFactory.create(controllers.map { it.agent }) }
    val render = remember(cartridge) { cartridge.renderFactory.create() }
    val state = remember(engine) { MutableStateFlow<ImageBitmap?>(null) }
    val image by state.collectAsState()

    LaunchedEffect(engine) {
        while (true) {
            when (val result = engine.advance()) {
                EngineResult.Complete -> {
                    break
                }

                is EngineResult.Running -> {
                    val stateData = result.state.serialize()
                    val imageData = render.render(stateData)
                    state.value = imageData.toImageBitmap()
                    delay(1_000 / 32)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = "",
                modifier = Modifier.border(1.0.dp, Color.Red),
            )
        }
    }
}

fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
//    val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
//    return bitmap.asImageBitmap()
}
