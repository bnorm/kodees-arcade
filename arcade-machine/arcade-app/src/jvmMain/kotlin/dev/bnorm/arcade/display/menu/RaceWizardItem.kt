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
import dev.bnorm.arcade.display.ArcadeMenuItem
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.rally.RaceWizardScreen
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(WindowScope::class)
class RaceWizardItem(
    private val gameViewModel: GameViewModel,
    private val raceWizardScreen: RaceWizardScreen,
) : ArcadeMenuItem {
    override val category get() = ArcadeMenuItem.Category.Race
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
                raceWizardScreen.Content(
                    onStart = {
                        gameViewModel.new(it)
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
