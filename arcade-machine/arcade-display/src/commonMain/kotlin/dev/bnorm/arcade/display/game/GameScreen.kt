package dev.bnorm.arcade.display.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.car.CarBackground
import dev.bnorm.arcade.display.car.CarColor
import dev.bnorm.arcade.display.car.CarTop
import dev.bnorm.arcade.display.game.driver.DriverDebugViewModel
import dev.bnorm.arcade.display.game.driver.Drivers
import dev.bnorm.arcade.display.internal.FixedSize
import dev.bnorm.arcade.display.internal.LogarithmicSlider
import dev.bnorm.arcade.display.track.TrackImage
import dev.bnorm.arcade.display.track.toOffset
import dev.zacsweers.metro.Inject

@Inject
class GameScreen(
    private val gameViewModel: GameViewModel,
    private val driverDebugViewModel: DriverDebugViewModel,
) {
    @Composable
    fun Content() {
        GameScreen(gameViewModel, driverDebugViewModel, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    driverDebugViewModel: DriverDebugViewModel,
    modifier: Modifier = Modifier,
    showDebug: Boolean = true,
) {
    val model by gameViewModel.models.collectAsState()

    val complete = model.complete
    var showResults by remember(complete) { mutableStateOf(true) }
    if (complete != null && showResults) {
        BasicAlertDialog(
            onDismissRequest = {
                showResults = false
                gameViewModel.clear()
            },
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
            ) {
                RaceResults(
                    results = complete.results,
                    modifier = Modifier
                        .padding(16.dp)
                )
            }
        }
    }

    val shape = MaterialTheme.shapes.large
    val shadowElevation = 4.dp
    val padding = 16.dp

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            Modifier
                .padding(padding)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = shape,
                    shadowElevation = shadowElevation,
                    color = Color(red = 0x00, green = 0x55, blue = 0x00),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Game(model, modifier = Modifier.fillMaxSize())
                }

                if (showDebug) {
                    Spacer(Modifier.width(padding))

                    Surface(
                        shape = shape,
                        shadowElevation = shadowElevation,
                    ) {
                        Drivers(model, driverDebugViewModel)
                    }
                }
            }

            Spacer(Modifier.height(padding))

            Surface(
                shape = shape,
                shadowElevation = shadowElevation,
            ) {
                GameControls(model, gameViewModel)
            }
        }
    }
}

@Composable
private fun GameControls(
    model: GameModel,
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .padding(16.dp)
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

        Button(
            enabled = model.active && !model.running,
            onClick = { gameViewModel.step() }
        ) {
            Text("Step")
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

@Composable
private fun Game(
    model: GameModel,
    modifier: Modifier = Modifier,
) {
    // TODO FixedSize is causing the Canvas rotate+translate on the car vector to be incredibly blurry.
    //  Instead, we should do all the translation math needed to correctly scale and locate.
    //  Or maybe adjust density?
    val carBackground = rememberVectorPainter(image = CarBackground)
    val carColor = rememberVectorPainter(image = CarColor)
    val carTop = rememberVectorPainter(image = CarTop)
    val colors = remember {
        // TODO add more colors!
        // TODO allow driver to pick colors?!
        listOf(
            Color.Blue,
            Color.Green,
            Color.Magenta,
            Color.Red,
            Color.Cyan,
            Color.Yellow,
        )
    }

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
        TrackImage(track, modifier = Modifier.fillMaxSize())

        val textMeasurer = rememberTextMeasurer()
        val nameMeasureResults = remember(names) {
            names.map { textMeasurer.measure(it) }
        }

        val carSize = Size(27.2f, 20f)
        Canvas(Modifier.fillMaxSize()) {
            val drivers = model.update?.drivers.orEmpty()
            for ((index, position) in drivers.withIndex()) {
                val x = position.x.toFloat()
                val y = size.height - position.y.toFloat()
                val center = Offset(x, y)


                val result = nameMeasureResults[index]
                val textOffset = Offset(
                    x = -result.size.width / 2f,
                    y = carSize.height / 2f * 0.4f,
                )
                drawText(result, color = Color.Black, topLeft = center + textOffset)
            }

            for ((index, position) in drivers.withIndex()) {
                val center = position.toOffset()
                rotate(degrees = -position.heading.degrees.toFloat(), pivot = center) {
                    val tint = ColorFilter.tint(color = colors[index % colors.size], BlendMode.SrcIn)
                    translate(left = center.x - carSize.width / 2, top = center.y - carSize.height / 2) {
                        with(carBackground) { draw(carSize) }
                        with(carColor) { draw(carSize, colorFilter = tint) }
                        with(carTop) { draw(carSize) }
                    }
                }
            }

            for (driver in drivers) {
                driver.debug?.drawRequests?.forEach { request ->
                    request.draw()
                }
            }
        }
    }
}
