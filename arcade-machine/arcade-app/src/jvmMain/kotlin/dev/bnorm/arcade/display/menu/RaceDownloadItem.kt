package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberDialogState
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.rally.RaceDownloader
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(WindowScope::class)
class RaceDownloadItem(
    private val client: ArcadeClient,
    private val gameViewModel: GameViewModel,
) : MenuItem {
    override val category get() = MenuItem.Category.Race
    override val order get() = 3

    @Composable
    override fun MenuScope.Content() {
        val state = rememberDialogState(
            size = DpSize(400.dp, 300.dp),
            position = WindowPosition.PlatformDefault,
        )

        var visible by remember { mutableStateOf(false) }
        if (visible) {
            DialogWindow(
                title = "Download Race",
                state = state,
                onCloseRequest = { visible = false }
            ) {
                RaceDownloader(
                    client,
                    onStart = {
                        gameViewModel.new(it)
                        visible = false
                    },
                    onError = {
                        visible = false
                    },
                )
            }
        }

        Item(
            text = "Download",
            onClick = {
                gameViewModel.clear()
                visible = true
            }
        )
    }
}
