package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.WindowScope
import dev.bnorm.arcade.display.ArcadeMenuItem
import dev.bnorm.arcade.display.window.WatchWindow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesIntoSet(WindowScope::class)
class WatchedDirectoryItem(
    private val window: WatchWindow,
) : ArcadeMenuItem {
    override val category get() = ArcadeMenuItem.Category.Settings
    override val order get() = 1

    @Composable
    override fun MenuScope.Content() {
        Item(
            text = "Watched Directories",
            onClick = {
                window.visible = true
            }
        )
    }
}
