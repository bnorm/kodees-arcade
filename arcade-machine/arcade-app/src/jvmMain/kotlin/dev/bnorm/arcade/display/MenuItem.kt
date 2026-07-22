package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope

interface MenuItem {
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

@Composable
fun FrameWindowScope.InstallMenuItems(items: Iterable<MenuItem>) {
    MenuBar {
        val groups = items.groupBy { it.category }.toSortedMap()
        for ((key, values) in groups) {
            Menu(key.name) {
                for (item in values.sortedBy { it.order }) {
                    with(item) {
                        Content()
                    }
                }
            }
        }
    }
}
