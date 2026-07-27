package dev.bnorm.arcade.display.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.bnorm.arcade.app.WindowGraph
import dev.bnorm.arcade.display.ArcadeMenuItem
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class ArcadeMenuBar(
    private val windowGraphFactory: WindowGraph.Factory,
) {
    @Composable
    context(scope: FrameWindowScope)
    fun Content() {
        val windowGraph = remember { windowGraphFactory.create(scope) }
        InstallMenuItems(windowGraph.menuItems)
    }

    @Composable
    context(scope: FrameWindowScope)
    private fun InstallMenuItems(items: Iterable<ArcadeMenuItem>) {
        scope.MenuBar {
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
}
