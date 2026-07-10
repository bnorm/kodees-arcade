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
import dev.bnorm.arcade.display.TrackViewModel
import dev.bnorm.arcade.rally.track.TrackDownloader
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(WindowScope::class)
class TrackDownloadItem(
    private val client: ArcadeClient,
    private val trackViewModel: TrackViewModel,
) : MenuItem {
    override val category get() = MenuItem.Category.Track
    override val order get() = 2

    @Composable
    override fun MenuScope.Content() {
        val state = rememberDialogState(
            size = DpSize(400.dp, 300.dp),
            position = WindowPosition.PlatformDefault,
        )

        var visible by remember { mutableStateOf(false) }
        if (visible) {
            DialogWindow(
                title = "Track Download",
                state = state,
                onCloseRequest = { visible = false }
            ) {
                TrackDownloader(
                    client,
                    onDownload = {
                        trackViewModel.new(it)
                        visible = false
                    }
                )
            }
        }

        Item(
            text = "Download",
            onClick = {
                visible = true
            }
        )
    }
}
