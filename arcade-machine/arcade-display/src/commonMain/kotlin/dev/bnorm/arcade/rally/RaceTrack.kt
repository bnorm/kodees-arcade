package dev.bnorm.arcade.rally

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.bnorm.arcade.arcade_display.generated.resources.Res
import dev.bnorm.arcade.arcade_display.generated.resources.car_blue
import dev.bnorm.arcade.arcade_display.generated.resources.car_grey
import dev.bnorm.arcade.arcade_display.generated.resources.car_orange
import dev.bnorm.arcade.arcade_display.generated.resources.car_purple
import dev.bnorm.arcade.arcade_display.generated.resources.car_red
import dev.bnorm.arcade.arcade_display.generated.resources.car_teal
import dev.bnorm.arcade.arcade_display.generated.resources.car_yellow
import dev.bnorm.arcade.display.GameModel
import dev.bnorm.arcade.display.GameViewModel
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.track.Track
import org.jetbrains.compose.resources.imageResource

@Composable
fun Game(
    gameViewModel: GameViewModel,
    onComplete: (Game.Event.Complete) -> Unit,
    modifier: Modifier = Modifier,
) {
    val model by gameViewModel.models.collectAsState()
    Column(
        modifier
            .height(IntrinsicSize.Min)
    ) {
        Game(
            model = model,
            onComplete = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Button(
                enabled = model.active,
                onClick = {
                    gameViewModel.stop()
                }
            ) {
                Text("Stop!")
            }

            Button(
                enabled = model.active,
                onClick = { gameViewModel.togglePlayPause() }
            ) {
                Text(if (model.running) "Pause" else "Play")
            }

            Text(
                text = "FPS: ${model.desiredUps.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(96.dp)
            )

            LogarithmicSlider(
                initialValue = model.desiredUps,
                maxValue = 10000f,
                onValueChange = { gameViewModel.adjustUps(it) },
                steps = 50,
            )
        }
    }
}

@Composable
private fun Game(
    model: GameModel,
    onComplete: (Game.Event.Complete) -> Unit,
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

    val track = model.start.track
    val names = model.start.drivers
    FixedSize(
        size = IntSize(
            width = track.width.toInt(),
            height = track.height.toInt(),
        ),
        density = Density(1f),
        modifier = modifier
    ) {
        Track(track, modifier = Modifier.fillMaxSize())

        val textMeasurer = rememberTextMeasurer()
        val nameMeasureResults = remember(names) {
            names.map { textMeasurer.measure(it) }
        }
        Canvas(Modifier.fillMaxSize()) {
            val positions = model.update?.drivers.orEmpty()
            for ((index, position) in positions.withIndex()) {
                val x = position.x.toFloat()
                val y = size.height - position.y.toFloat()
                val center = Offset(x, y)

                val image = images[index % images.size]

                val result = nameMeasureResults[index]
                val textOffset = Offset(
                    x = -result.size.width / 2f,
                    y = image.height.toFloat() / 2f * 0.4f,
                )
                drawText(result, color = Color.Black, topLeft = center + textOffset)
            }

            for ((index, position) in positions.withIndex()) {
                val x = position.x.toFloat()
                val y = size.height - position.y.toFloat()
                val center = Offset(x, y)

                val heading = 90f - position.heading.toRelative().degrees.toFloat()

                val image = images[index % images.size]
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
}
