package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope

interface ArcadeMenuItem {
    val category: Category
    val order: Int

    @Composable
    fun MenuScope.Content()

    enum class Category {
        Race,
        Track,
        Settings,
    }
}
