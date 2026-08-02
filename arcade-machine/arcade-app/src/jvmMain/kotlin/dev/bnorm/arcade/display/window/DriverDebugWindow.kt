package dev.bnorm.arcade.display.window

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.ArcadeWindow
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.game.TextLines
import dev.bnorm.arcade.display.game.driver.DriverDebugViewModel
import dev.bnorm.arcade.display.game.driver.DriverTerminal
import dev.bnorm.arcade.display.menu.ArcadeMenuBar
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@ContributesIntoSet(AppScope::class)
class DriverDebugWindow(
    private val arcadeMenuBar: ArcadeMenuBar,
    private val debugViewModel: DriverDebugViewModel,
    private val gameViewModel: GameViewModel,
) : ArcadeWindow {
    @Composable
    override fun ApplicationScope.Content() {
        val game by gameViewModel.models.collectAsState()

        for (name in game.start.drivers) {
            key(name) {
                if (name in debugViewModel.open) {
                    Window(
                        title = name,
                        state = rememberWindowState(width = 600.dp, height = 400.dp),
                        onCloseRequest = {
                            debugViewModel.close(name)
                        }
                    ) {
                        arcadeMenuBar.Content()

                        LaunchedEffect(Unit) {
                            debugViewModel.focusRequests.filter { it.driver == name }.collectLatest {
                                window.toFront()
                                window.requestFocus()
                            }
                        }

                        Column {
                            Surface(
                                color = Color.Black,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                DriverTerminal(
                                    output = game.driverOutput[name] ?: TextLines(),
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val checked = name in debugViewModel.drawing
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        when (checked) {
                                            true -> debugViewModel.stopDrawing(name)
                                            false -> debugViewModel.startDrawing(name)
                                        }
                                    }
                                )
                                Text("Drawing Enabled", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
