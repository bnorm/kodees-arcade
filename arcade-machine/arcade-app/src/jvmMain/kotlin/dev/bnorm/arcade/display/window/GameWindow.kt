package dev.bnorm.arcade.display.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.app.WindowGraph
import dev.bnorm.arcade.display.ArcadeWindow
import dev.bnorm.arcade.display.InstallMenuItems
import dev.bnorm.arcade.display.game.GameScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(AppScope::class)
class GameWindow(
    private val windowGraphFactory: WindowGraph.Factory,
    private val gameScreen: GameScreen
) : ArcadeWindow {
    @Composable
    override fun ApplicationScope.Content() {
        Window(
            title = "Kodee's Arcade",
            state = rememberWindowState(width = 1600.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            val windowGraph = remember(windowGraphFactory) {
                windowGraphFactory.create(this@Window)
            }

            InstallMenuItems(windowGraph.items)
            gameScreen.Content()
        }
    }
}
