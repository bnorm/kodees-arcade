package dev.bnorm.arcade.rally

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.display.AvailableDriverViewModel
import dev.bnorm.arcade.display.asset.icon.progress_activity
import dev.bnorm.arcade.display.track.TrackImage
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.engine.WasmDriver
import dev.bnorm.arcade.rally.engine.WasmGame
import dev.bnorm.arcade.rally.engine.WasmModule
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.Version
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
class RaceWizardScreen(
    private val client: ArcadeClient,
    private val trackViewModel: TrackViewModel,
    private val availableDriverViewModel: AvailableDriverViewModel? = null,
) {
    private val compilerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val moduleCache = mutableStateMapOf<Any, Deferred<WasmModule>>()
    private inner class SelectedDriver(
        val name: String,
        module: Deferred<WasmModule>,
    ) {
        var ready by mutableStateOf(false)
            private set

        var error by mutableStateOf<Throwable?>(null)
            private set

        private var module: Deferred<WasmModule> = compilerScope.async {
            try {
                module.await()
            } catch (t: Throwable) {
                error = t
                throw t
            } finally {
                ready = true
            }
        }

        suspend fun createWasmDriver(): WasmDriver {
            return module.await().createDriver(name)
        }
    }

    private var selectedTrack by mutableStateOf<Track?>(null)
    private var laps by mutableIntStateOf(25)
    private val drivers = mutableStateListOf<SelectedDriver>()

    private fun pickDriverName(baseName: String): String {
        val existingNames = drivers.mapTo(mutableSetOf()) { it.name }
        var name = baseName
        var i = 1
        while (name in existingNames) {
            name = baseName + " (${i++})"
        }
        return name
    }

    private fun selectDriverFile(file: PlatformFile) {
        val module = compilerScope.async {
            WasmModule(file.readBytes())
        }
        drivers.add(SelectedDriver(pickDriverName(file.nameWithoutExtension), module))
    }

    private fun selectDriverDownload(name: String, id: DriverId, version: Version) {
        val module = moduleCache.getOrPut(id to version) {
            compilerScope.async {
                WasmModule(client.downloadDriverVersion(id, version))
            }
        }
        drivers.add(SelectedDriver(pickDriverName(name), module))
    }

    private fun validDrivers(): Boolean {
        val selectedTrack = selectedTrack
        return selectedTrack != null &&
            drivers.size in 1..selectedTrack.positions.size &&
            drivers.all { it.ready && it.error == null }
    }

    private fun validLaps(): Boolean {
        return laps > 0
    }

    @Composable
    fun Content(
        onStart: (Game) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .padding(8.dp)
        ) {
            val model by trackViewModel.models.collectAsState()
            TrackSelector(
                tracks = model.tracks,
                selectedTrack = selectedTrack,
                onTrackSelected = { selectedTrack = it },
            )
            Laps()
            DriverSelection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            StartButton(
                onStart = onStart,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
    }

    @Composable
    private fun Laps() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Laps:", style = MaterialTheme.typography.titleLarge)
            IntNumberField(
                initial = laps,
                onValueChange = { laps = it ?: 0 },
                isError = !validLaps(),
            )
        }
    }

    @Composable
    private fun DriverSelection(
        modifier: Modifier = Modifier,
    ) {
        val driversLauncher = rememberFilePickerLauncher(
            mode = FileKitMode.Single,
            type = FileKitType.File("wasm"),
        ) { file ->
            if (file != null) {
                selectDriverFile(file)
            }
        }

        class DriverDisplay(
            val id: DriverId,
            val version: Version,
            val name: String,
        )

        var showDownloader by remember { mutableStateOf(false) }
        val serverDrivers = remember { mutableStateListOf<DriverDisplay>() }
        if (showDownloader) {
            LaunchedEffect(Unit) {
                try {
                    val foundDrivers = client.getDrivers()
                    val foundVersions = foundDrivers
                        .flatMap { driver ->
                            client.getDriverVersions(driver.id)
                                .map { DriverDisplay(driver.id, it.version, driver.name) }
                        }
                    serverDrivers.clear()
                    serverDrivers.addAll(foundVersions)
                } catch (_: Throwable) {
                }
            }

            Dialog(onDismissRequest = { showDownloader = false }) {
                Surface(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Drivers", style = MaterialTheme.typography.headlineSmall)
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(serverDrivers) { driver ->
                                val version = driver.version
                                val name = "${driver.name} $version"
                                Text(
                                    text = name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectDriverDownload(name, driver.id, version)
                                            showDownloader = false
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        driversLauncher.launch()
                    }
                ) {
                    Text("Load Driver")
                }
                Button(
                    onClick = {
                        showDownloader = true
                    }
                ) {
                    Text("Download")
                }
            }

            Row {
                SelectedDrivers(selectedTrack, modifier = Modifier.weight(1f))
                AvailableDrivers(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun SelectedDrivers(
        selectedTrack: Track?,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier) {
            Text("Selected Drivers:", style = MaterialTheme.typography.titleLarge)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val positions =
                    if (selectedTrack != null) maxOf(drivers.size, selectedTrack.positions.size) else drivers.size
                repeat(positions) { position ->
                    val driver = drivers.getOrNull(position)
                    SelectedDriver(selectedTrack, position, driver)
                }
            }
        }
    }

    @Composable
    private fun SelectedDriver(
        selectedTrack: Track?,
        position: Int,
        driver: SelectedDriver?
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .height(40.dp) // TODO can we make the icons smaller?
        ) {
            val validPosition = selectedTrack != null && position < selectedTrack.positions.size
            Text(
                text = "P${position + 1}",
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(32.dp)
                    .visible(validPosition)
            )

            if (driver != null) {
                Text(
                    text = driver.name,
                    color = if (validPosition) Color.Unspecified else MaterialTheme.colorScheme.error,
                )

                val error = driver.error
                if (!driver.ready) {
                    // TODO size the icon together with the text
                    val transition = rememberInfiniteTransition()
                    val rotation by transition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1000))
                    )
                    Icon(
                        painter = rememberVectorPainter(progress_activity),
                        contentDescription = null,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = rotation }
                    )
                } else if (error != null) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below
                        ),
                        state = rememberTooltipState(isPersistent = true),
                        tooltip = {
                            PlainTooltip {
                                Text(error.stackTraceToString())
                            }
                        },
                    ) {
                        // TODO icon
                        Text("!!")
                    }
                }

                Spacer(Modifier.weight(1f))

                Row {
                    // TODO drag and drop might be better?
                    IconButton(
                        onClick = {
                            val tmp = drivers[position]
                            drivers[position] = drivers[position + 1]
                            drivers[position + 1] = tmp

                        },
                        modifier = Modifier
                            .visible(position != drivers.lastIndex)
                    ) {
                        // TODO icon
                        Text("D")
                    }
                    IconButton(
                        onClick = {
                            val tmp = drivers[position]
                            drivers[position] = drivers[position - 1]
                            drivers[position - 1] = tmp
                        },
                        modifier = Modifier
                            .visible(position != 0)
                    ) {
                        // TODO icon
                        Text("U")
                    }
                    IconButton(
                        onClick = {
                            // TODO wrap with snapshot?
                            drivers.removeAt(position)
                        },
                        modifier = Modifier
                    ) {
                        // TODO icon
                        Text("X")
                    }
                }
            }
        }
    }

    @Composable
    fun AvailableDrivers(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            Text("Available Drivers:", style = MaterialTheme.typography.titleLarge)
            if (availableDriverViewModel != null) {
                val model by availableDriverViewModel.models.collectAsState()
                for (driver in model.drivers) {
                    Button(
                        onClick = {
                            selectDriverFile(driver.file)
                        }
                    ) {
                        Text(driver.name)
                    }
                }
            }
        }
    }

    @Composable
    private fun StartButton(
        onStart: (Game) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val scope = rememberCoroutineScope()
        val selectedTrack = selectedTrack
        val enabled = selectedTrack != null && validDrivers() && validLaps()
        Button(
            enabled = enabled,
            onClick = {
                if (enabled) {
                    scope.launch {
                        val drivers = drivers.map { it.createWasmDriver() }
                        val game = WasmGame(selectedTrack, drivers, laps)
                        onStart(game)
                    }
                }
            },
            modifier = modifier
        ) {
            Text("Start!")
        }
    }
}

@Composable
private fun TrackSelector(
    tracks: List<Track>,
    selectedTrack: Track?,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text("Available Tracks:", style = MaterialTheme.typography.titleLarge)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            for (track in tracks) {
                TrackImage(
                    track = track,
                    modifier = Modifier
                        .size(200.dp, 200.dp)
                        .border(
                            width = 4.dp,
                            color = when (track) {
                                selectedTrack -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp)
                        .clickable { onTrackSelected(track) }
                )

            }
        }
    }
}

@Composable
fun IntNumberField(
    initial: Int,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    isError: Boolean = false,
    contentPadding: PaddingValues = TextFieldDefaults.contentPaddingWithoutLabel()
) {
    val text = rememberTextFieldState(initial.toString())
    LaunchedEffect(text) {
        snapshotFlow { text.text.toString() }.collectLatest {
            onValueChange(it.toIntOrNull())
        }
    }

    TextField(
        state = text,
        modifier = modifier,
        label = label,
        isError = isError,
        inputTransformation = InputTransformation {
            if (asCharSequence().any { !it.isDigit() }) {
                revertAllChanges()
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        contentPadding = contentPadding,
    )
}
