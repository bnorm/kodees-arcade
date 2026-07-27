package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.WindowScope
import dev.bnorm.arcade.display.AvailableDriverViewModel
import dev.bnorm.arcade.display.ArcadeMenuItem
import dev.zacsweers.metro.ContributesIntoSet
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher

@ContributesIntoSet(WindowScope::class)
class WatchedDirectoryItem(
    private val availableDriverViewModel: AvailableDriverViewModel,
    private val scope: WindowScope,
) : ArcadeMenuItem {
    override val category get() = ArcadeMenuItem.Category.Settings
    override val order get() = 1

    @Composable
    override fun MenuScope.Content() {
        availableDriverViewModel.models.collectAsState()

        val watchPicker = scope.rememberDirectoryPickerLauncher { dir ->
            if (dir != null) {
                availableDriverViewModel.watch(dir)
            }
        }

        Item(
            text = "Watch",
            onClick = {
                watchPicker.launch()
            }
        )
    }
}
