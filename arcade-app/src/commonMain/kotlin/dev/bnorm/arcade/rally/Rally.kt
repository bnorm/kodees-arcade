package dev.bnorm.arcade.rally

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_app.generated.resources.*
import dev.bnorm.arcade.arcade_samples.generated.resources.BundledRacers
import dev.bnorm.arcade.rally.engine.ByteArrayRacer
import dev.bnorm.arcade.rally.engine.Racer
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.game
import dev.bnorm.arcade.geometry.toRelative
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun SampleRally() {
    var track by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(Unit) {
        track = loadTrack()
    }

    val carImages = listOf(
        Res.drawable.car_blue,
        Res.drawable.car_grey,
        Res.drawable.car_orange,
        Res.drawable.car_purple,
        Res.drawable.car_red,
        Res.drawable.car_teal,
        Res.drawable.car_yellow,
    ).map { imageResource(it) }

    track?.let {
        Rally(carImages, it)
    }
}

private val BUNDLED_RACERS = listOf("Kodee", "Snail")

@Composable
fun Rally(
    carImages: List<ImageBitmap>,
    track: Track
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
                    ByteArrayRacer(
                        name = pickRacerName(file.name.substringBeforeLast(".")),
                        bytes = file.readBytes()
                    )
                )
            }
        }
    }

    var game by remember { mutableStateOf<ReceiveChannel<RallyGameState>?>(null) }

    Column {
        var desiredFps by remember { mutableFloatStateOf(60f) }
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
                            scope.launch {
                                racers.add(
                                    ByteArrayRacer(
                                        name = pickRacerName(racer),
                                        bytes = BundledRacers.readBytes("files/$racer.wasm")
                                    )
                                )
                            }
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
        Row {
            Button(
                enabled = racers.isNotEmpty(),
                onClick = {
                    game?.cancel()
                    game = scope.game(
                        track = track,
                        racers = racers,
                        carImages = carImages,
                    )
                }
            ) {
                Text("Start!")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = game != null,
                onClick = {
                    game?.cancel()
                    game = null
                },
            ) {
                Text("Stop!")
            }
        }

        Spacer(Modifier.width(8.dp))
        FixedSize(
            size = IntSize(track.width.toInt(), track.height.toInt()),
            density = Density(1f),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Image(
                painter = painterResource(Res.drawable.track),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            Game(game, desiredFps)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "FPS: ${desiredFps.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(128.dp)
            )

            LogarithmicSlider(
                initialValue = desiredFps,
                maxValue = 10000f,
                onValueChange = { desiredFps = it },
                steps = 50,
            )
        }
    }
}

@Composable
private fun Game(
    game: ReceiveChannel<RallyGameState>?,
    desiredFps: Float
) {
    var state by remember { mutableStateOf<RallyGameState?>(null) }

    LaunchedEffect(game, desiredFps) {
        if (game == null) {
            state = null
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {
            state = game.receive()

            val frameDelay = (1.0 / desiredFps).seconds
            val startTime = TimeSource.Monotonic.markNow()
            var targetTime = 0.seconds

            while (isActive) {
                val next = game.receive()

                val currentTime = startTime.elapsedNow()
                val delay = targetTime - currentTime
                if (delay > Duration.ZERO) delay(delay)
                targetTime += frameDelay

                state = next
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        for ((name, state) in state?.racers?.entries.orEmpty()) {
            val x = state.x.toFloat()
            val y = size.height - state.y.toFloat()
            val center = Offset(x, y)

            val image = state.image

            val result = textMeasurer.measure(name)
            val textOffset = Offset(
                x = -result.size.width / 2f,
                y = image.height.toFloat() / 2f * 0.4f,
            )
            drawText(result, color = Color.Black, topLeft = center + textOffset)
        }

        for ((_, state) in state?.racers?.entries.orEmpty()) {
            val x = state.x.toFloat()
            val y = size.height - state.y.toFloat()
            val center = Offset(x, y)

            val heading = 90f - state.heading.toRelative().degrees.toFloat()

            val image = state.image
            val imageSize = Offset(
                x = image.width.toFloat(),
                y = image.height.toFloat(),
            )

            rotate(degrees = heading, pivot = center) {
                scale(scale = 0.4f, pivot = center) {
                    drawImage(image, topLeft = center - (imageSize / 2f))
                }
            }
        }
    }
}