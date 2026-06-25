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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.arcade_app.generated.resources.*
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.rally.race.Race
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun RaceTrack(
    track: Track,
    race: Race?,
    onComplete: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var desiredFps by remember { mutableFloatStateOf(60f) }

    Column(modifier) {
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

            Game(race, desiredFps, onComplete)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Button(
                enabled = race != null,
                onClick = {
                    onStop()
                }
            ) {
                Text("Stop!")
            }

            Text(
                text = "FPS: ${desiredFps.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(96.dp)
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
    race: Race?,
    desiredFps: Float,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = remember {
        listOf(
            Res.drawable.car_blue,
            Res.drawable.car_grey,
            Res.drawable.car_orange,
            Res.drawable.car_purple,
            Res.drawable.car_red,
            Res.drawable.car_teal,
            Res.drawable.car_yellow,
        )
    }.map { imageResource(it) }

    var state by remember { mutableStateOf<Race.Event.Update?>(null) }

    LaunchedEffect(race, desiredFps) {
        if (race == null) {
            state = null
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {
            val frameDelay = (1.0 / desiredFps).seconds
            val startTime = TimeSource.Monotonic.markNow()
            var targetTime = 0.seconds

            while (isActive) {
                val result = race.events.receiveCatching()

                val currentTime = startTime.elapsedNow()
                val delay = targetTime - currentTime
                if (delay > Duration.ZERO) delay(delay)
                targetTime = maxOf(targetTime + frameDelay, currentTime)

                if (result.isClosed) break
                when (val next = result.getOrThrow()) {
                    is Race.Event.Complete -> {
                        onComplete()
                        break
                    }

                    is Race.Event.Start -> continue
                    is Race.Event.Update -> {
                        state = next
                    }
                }
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxSize()) {
        for ((index, entry) in state?.racers?.entries.orEmpty().withIndex()) {
            val (name, state) = entry
            val x = state.x.toFloat()
            val y = size.height - state.y.toFloat()
            val center = Offset(x, y)

            val image = images[index]

            val result = textMeasurer.measure(name)
            val textOffset = Offset(
                x = -result.size.width / 2f,
                y = image.height.toFloat() / 2f * 0.4f,
            )
            drawText(result, color = Color.Black, topLeft = center + textOffset)
        }

        for ((index, state) in state?.racers?.values.orEmpty().withIndex()) {
            val x = state.x.toFloat()
            val y = size.height - state.y.toFloat()
            val center = Offset(x, y)

            val heading = 90f - state.heading.toRelative().degrees.toFloat()

            val image = images[index]
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
