package dev.bnorm.arcade.display.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.ArcadeWindow
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.menu.ArcadeMenuBar
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(AppScope::class)
class GameWindow(
    private val arcadeMenuBar: ArcadeMenuBar,
    private val gameScreen: GameScreen,
) : ArcadeWindow {
    @Composable
    override fun ApplicationScope.Content() {
        Window(
            title = "Kodee's Arcade",
            state = rememberWindowState(width = 1600.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            arcadeMenuBar.Content()
            gameScreen.Content()
        }
    }
}
