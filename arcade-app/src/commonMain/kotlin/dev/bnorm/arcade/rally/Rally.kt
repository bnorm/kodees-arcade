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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_app.generated.resources.Res
import dev.bnorm.arcade.arcade_app.generated.resources.car_blue
import dev.bnorm.arcade.arcade_app.generated.resources.car_grey
import dev.bnorm.arcade.arcade_app.generated.resources.car_orange
import dev.bnorm.arcade.arcade_app.generated.resources.car_purple
import dev.bnorm.arcade.arcade_app.generated.resources.car_red
import dev.bnorm.arcade.arcade_app.generated.resources.car_teal
import dev.bnorm.arcade.arcade_app.generated.resources.car_yellow
import dev.bnorm.arcade.arcade_app.generated.resources.track
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.game
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

@Composable
fun Rally(
    carImages: List<ImageBitmap>,
    track: Track
) {
    val scope = rememberCoroutineScope()
    var game by remember { mutableStateOf<ReceiveChannel<RallyGameState>?>(null) }

    val racers = remember { mutableStateMapOf<String, PlatformFile>() }
    val racersLauncher = rememberFilePickerLauncher(
        mode = PickerMode.Single,
        type = PickerType.File(listOf("wasm")),
        initialDirectory = "../arcade-samples/build/racers",
    ) { file ->
        if (file != null) {
            var name = file.name.substringBeforeLast(".")
            if (name in racers) name = "$name (1)"
            var i = 1
            while (name in racers) {
                name = name.substringBeforeLast(" ") + " (${i++})"
            }
            racers[name] = file
        }
    }

    Column {
        var desiredFps by remember { mutableFloatStateOf(60f) }
        Row {
            Button(
                enabled = racers.size < 6,
                onClick = {
                    racersLauncher.launch()
                }
            ) {
                Text("Pick Racer")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = racers.isNotEmpty(),
                onClick = {
                    racers.clear()
                }
            ) {
                Text("Clear")
            }
        }

        for ((name, _) in racers) {
            Spacer(Modifier.width(8.dp))
            Text(name)
        }

        Spacer(Modifier.width(8.dp))
        Row {
            Button(
                enabled = racers.isNotEmpty(),
                onClick = {
                    game?.cancel()
                    game = scope.game(
                        carImages = carImages,
                        track = track,
                        paths = racers,
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
            modifier = Modifier.fillMaxWidth()
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

    Canvas(Modifier.fillMaxSize()) {
        for (state in state?.racers?.values.orEmpty()) {
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
//                drawRect(
//                    color = Color.Red,
//                    topLeft = Offset(
//                        x = x - (carWidth / 2).toFloat(),
//                        y = y - (carHeight / 2).toFloat(),
//                    ),
//                    size = Size(
//                        width = carWidth.toFloat(),
//                        height = carHeight.toFloat(),
//                    )
//                )
            }
        }
    }
}