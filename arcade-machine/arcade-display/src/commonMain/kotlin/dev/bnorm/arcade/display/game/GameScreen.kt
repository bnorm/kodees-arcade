package dev.bnorm.arcade.display.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import dev.bnorm.arcade.display.internal.FixedSize
import dev.bnorm.arcade.display.internal.LogarithmicSlider
import dev.bnorm.arcade.display.track.TrackImage
import dev.bnorm.arcade.display.track.toOffset
import dev.bnorm.arcade.machine.DrawRequest
import dev.zacsweers.metro.Inject

@Inject
class GameScreen(
    private val gameViewModel: GameViewModel,
) {
    @Composable
    fun Content() {
        GameScreen(gameViewModel, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val model by gameViewModel.models.collectAsState()

    var showResults by remember(model.complete) { mutableStateOf(true) }
    model.complete?.let {
        if (showResults) {
            BasicAlertDialog(
                onDismissRequest = {
                    showResults = false
                    gameViewModel.clear()
                },
            ) {
                Surface {
                    RaceResults(it)
                }
            }
        }
    }

    Column(
        modifier
            .height(IntrinsicSize.Min)
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Game(
                model = model,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.fillMaxHeight().width(2.dp).background(Color.Black))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .background(Color.LightGray)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                for (name in model.start.drivers) {
                    key(name) {
                        var debug by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(name, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = debug,
                                        onCheckedChange = {
                                            debug = !debug
                                            gameViewModel.setDebug(name, debug)
                                        }
                                    )
                                    Text("Debug", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(Color.LightGray)
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
                driver.debug?.canvasRequests?.forEach { request ->
                    request.draw()
                }
            }
        }
    }
}

context(scope: DrawScope)
private fun DrawRequest.draw() {
    when (this) {
        is DrawRequest.DrawSegment -> scope.drawLine(
            color = Color(color.toInt()),
            start = segment.start.toOffset(),
            end = segment.end.toOffset(),
        )

        is DrawRequest.DrawCircle -> scope.drawCircle(
            color = Color(color.toInt()),
            radius = circle.radius.toFloat(),
            center = circle.center.toOffset(),
            style = Stroke(width = Stroke.HairlineWidth),
        )

        is DrawRequest.FillCircle -> scope.drawCircle(
            color = Color(color.toInt()),
            radius = circle.radius.toFloat(),
            center = circle.center.toOffset(),
        )

        is DrawRequest.FillRect -> {
            scope.drawRect(
                color = Color(color.toInt()),
                topLeft = rectangle.center.toOffset() -
                    Offset(
                        rectangle.width.toFloat() / 2f,
                        rectangle.height.toFloat() / 2f
                    ),
                size = Size(rectangle.width.toFloat(), rectangle.height.toFloat()),
            )
        }
    }
}
