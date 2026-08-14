package dev.bnorm.arcade.display.window

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.ArcadeWindow
import dev.bnorm.arcade.display.AvailableDriverViewModel
import dev.bnorm.arcade.display.menu.ArcadeMenuBar
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class WatchWindow(
    private val arcadeMenuBar: ArcadeMenuBar,
    private val availableDriverViewModel: AvailableDriverViewModel,
) : ArcadeWindow {
    var visible by mutableStateOf(false)

    @Composable
    override fun ApplicationScope.Content() {
        if (!visible) return

        Window(
            title = "Watches",
            state = rememberWindowState(width = 600.dp, height = 400.dp),
            onCloseRequest = { visible = false },
        ) {
            arcadeMenuBar.Content()

            val model by availableDriverViewModel.models.collectAsState()

            val watchPicker = rememberDirectoryPickerLauncher { dir ->
                if (dir != null) {
                    availableDriverViewModel.watch(dir)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    val measurer = rememberTextMeasurer()
                    for (watched in model.watched) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                ) {
                                    // Calculate a prefix-truncated string which fits the width.
                                    val path = watched.directory.file.toPath()
                                    var removedPrefixes = 0
                                    var text = path.subpath(removedPrefixes, path.nameCount).toString()
                                    while (measurer.measure(text).size.width > constraints.maxWidth) {
                                        if (++removedPrefixes >= path.nameCount) break
                                        text = ".../${path.subpath(removedPrefixes, path.nameCount)}"
                                    }
                                    Text(
                                        text,
                                        overflow = TextOverflow.MiddleEllipsis,
                                        maxLines = 1,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        availableDriverViewModel.unwatch(watched.key)
                                    },
                                    modifier = Modifier
                                ) {
                                    Icon(
                                        imageVector = dev.bnorm.arcade.display.asset.icon.close,
                                        contentDescription = "Remove directory."
                                    )
                                }
                            }
                            for (driver in watched.drivers) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(start = 24.dp)
                                ) {
                                    Text(driver.name + ".wasm")
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        watchPicker.launch()
                    },
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}
