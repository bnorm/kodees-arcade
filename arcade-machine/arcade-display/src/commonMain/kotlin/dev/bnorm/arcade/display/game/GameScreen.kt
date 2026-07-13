package dev.bnorm.arcade.display.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
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
import androidx.compose.ui.text.font.FontFamily
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

    Column(modifier) {
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

            Drivers(
                model,
                gameViewModel,
                modifier = Modifier
            )
        }

        Spacer(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        GameControls(model, gameViewModel)
    }
}

@Composable
private fun GameControls(
    model: GameModel,
    gameViewModel: GameViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
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

@Composable
private fun Drivers(
    model: GameModel,
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val vertical = rememberScrollState()
    val debug = remember { mutableStateSetOf<String>() }

    Box(
        modifier
            .animateContentSize()
            .fillMaxHeight()
            .width(400.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(vertical)
                .padding(8.dp)
                .padding(end = 8.dp)
        ) {
            for (name in model.start.drivers) {
                key(name) {
                    DriverInfo(name, debug, gameViewModel, model)
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(vertical),
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun DriverInfo(
    name: String,
    debug: SnapshotStateSet<String>,
    gameViewModel: GameViewModel,
    model: GameModel
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val checked = name in debug
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        when (checked) {
                            true -> {
                                debug.remove(name)
                                gameViewModel.setDebug(name, false)
                            }

                            false -> {
                                debug.add(name)
                                gameViewModel.setDebug(name, true)
                            }
                        }
                    }
                )
                Text("Debug", style = MaterialTheme.typography.bodyLarge)
            }
            DriverTerminal(
                output = model.driverOutput[name].orEmpty(),
                modifier = Modifier
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun DriverTerminal(
    output: String,
    modifier: Modifier = Modifier,
) {
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()

    // TODO start by being pinned to the bottom
    //  - and stay at the bottom if pinned

    Surface(
        color = Color.Black,
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            val scrollbarStyle = LocalScrollbarStyle.current.copy(
                unhoverColor = Color.White.copy(alpha = 0.5f),
                hoverColor = Color.White.copy(alpha = 0.8f),
                minimalHeight = 32.dp,
            )

            Box(
                Modifier
                    .verticalScroll(vertical)
                    .horizontalScroll(horizontal)
                    .padding(bottom = scrollbarStyle.thickness, end = scrollbarStyle.thickness)
            ) {
                Text(
                    text = output.removeSuffix("\n"),
                    style = MaterialTheme.typography.bodySmall.let { it.copy(lineHeight = it.fontSize) },
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
            CompositionLocalProvider(
                LocalScrollbarStyle provides scrollbarStyle
            ) {
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(vertical),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = scrollbarStyle.thickness)
                )
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(horizontal),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(end = scrollbarStyle.thickness)
                )
            }
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
                driver.debug?.drawRequests?.forEach { request ->
                    request.draw()
                }
            }
        }
    }
}

