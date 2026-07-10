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
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.machine.RecordGame
import dev.bnorm.arcade.rally.RaceWizard
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.ContributesIntoSet
import java.nio.file.Paths

@ContributesIntoSet(WindowScope::class)
class RaceWizardItem(
    private val client: ArcadeClient,
    private val trackViewModel: TrackViewModel,
    private val gameViewModel: GameViewModel,
) : MenuItem {
    override val category get() = MenuItem.Category.Race
    override val order get() = 1

    @Composable
    override fun MenuScope.Content() {
        val state = rememberDialogState(
            size = DpSize(600.dp, 600.dp),
            position = WindowPosition.PlatformDefault,
        )

        var visible by remember { mutableStateOf(false) }
        if (visible) {
            DialogWindow(
                title = "Race Wizard",
                state = state,
                onCloseRequest = { visible = false }
            ) {
                RaceWizard(
                    client,
                    trackViewModel,
                    onStart = {
                        gameViewModel.new(RecordGame(it, Paths.get("./recording.race")))
                        visible = false
                    }
                )
            }
        }

        Item(
            text = "New",
            onClick = {
                visible = true
            }
        )
    }
}
