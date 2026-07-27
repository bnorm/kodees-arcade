package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.WindowScope
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.ArcadeMenuItem
import dev.bnorm.arcade.machine.ReplayGame
import dev.zacsweers.metro.ContributesIntoSet
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@ContributesIntoSet(WindowScope::class)
class RaceLoadItem(
    private val gameViewModel: GameViewModel,
    private val scope: WindowScope,
) : ArcadeMenuItem {
    override val category get() = ArcadeMenuItem.Category.Race
    override val order get() = 2

    @Composable
    override fun MenuScope.Content() {
        val recordingPicker = scope.rememberFilePickerLauncher(
            mode = FileKitMode.Single,
            type = FileKitType.File("race"),
        ) { file ->
            if (file != null) {
                gameViewModel.new(ReplayGame(file))
            }
        }

        Item(
            text = "Load",
            onClick = {
                gameViewModel.clear()
                recordingPicker.launch()
            }
        )
    }
}
