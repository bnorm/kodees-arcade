package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope

interface ArcadeWindow {
    @Composable
    fun ApplicationScope.Content()
}
