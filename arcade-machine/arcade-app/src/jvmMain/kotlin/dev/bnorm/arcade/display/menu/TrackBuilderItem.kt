package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberDialogState
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.display.track.TrackBuilder
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(WindowScope::class)
class TrackBuilderItem(
    private val trackViewModel: TrackViewModel,
) : MenuItem {
    override val category get() = MenuItem.Category.Track
    override val order get() = 1

    @Composable
    override fun MenuScope.Content() {
        val state = rememberDialogState(
            size = DpSize(800.dp, 800.dp),
            position = WindowPosition.PlatformDefault,
        )

        var visible by remember { mutableStateOf(false) }
        if (visible) {
            DialogWindow(
                title = "Track Builder",
                state = state,
                onCloseRequest = { visible = false }
            ) {
                TrackBuilder(
                    initialSize = IntSize(600, 600),
                    onSave = {
                        trackViewModel.new(it)
                        visible = false
                    }
                )
            }
        }

        Item(
            text = "Create",
            onClick = {
                visible = true
            }
        )
    }
}
