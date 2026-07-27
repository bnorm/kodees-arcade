package dev.bnorm.arcade.display.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import dev.bnorm.arcade.display.ArcadeWindow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(AppScope::class)
class DriverDebugWindow : ArcadeWindow {
    @Composable
    override fun ApplicationScope.Content() {
    }
}
