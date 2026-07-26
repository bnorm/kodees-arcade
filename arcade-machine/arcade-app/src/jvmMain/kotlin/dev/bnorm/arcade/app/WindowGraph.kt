package dev.bnorm.arcade.app

import androidx.compose.ui.window.WindowScope
import dev.bnorm.arcade.display.MenuItem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(WindowScope::class)
interface WindowGraph {
    val items: Set<MenuItem>

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun create(
            @Provides windowScope: WindowScope
        ): WindowGraph
    }
}
