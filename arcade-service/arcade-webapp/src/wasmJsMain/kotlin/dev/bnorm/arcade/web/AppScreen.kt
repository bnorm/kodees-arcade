package dev.bnorm.arcade.web

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.github.terrakok.navigation3.browser.ChronologicalBrowserNavigation
import dev.bnorm.arcade.web.components.ArcadeScaffold
import dev.bnorm.arcade.web.route.BaseRouteKey
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.RouteScreen
import dev.bnorm.arcade.web.route.route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass

@Inject
@SingleIn(AppScope::class)
class AppScreen(
    private val backStack: NavBackStack<RouteKey>,
    private val baseRouteKeys: Set<BaseRouteKey>,
    screens: Set<RouteScreen<RouteKey>>,
) {
    private val routeScreens: Map<KClass<out RouteKey>, RouteScreen<RouteKey>> = buildMap {
        for (screen in screens) {
            @Suppress("UNCHECKED_CAST")
            put(screen.key, screen)
        }
    }

    @Composable
    fun Content() {
        ChronologicalBrowserNavigation(
            backStack = backStack,
            saveKey = { it.buildFragment() },
            restoreKey = { baseRouteKeys.route(it) }
        )

        Surface {
            ArcadeScaffold(
                backStack = backStack,
            ) {
                NavDisplay(backStack) { key ->
                    val screen = routeScreens.getValue(key::class)
                    screenNavEntry(screen, key)
                }
            }
        }
    }

    private fun <T : RouteKey> screenNavEntry(screen: RouteScreen<T>, key: T): NavEntry<T> {
        return NavEntry(key) {
            screen.Content(key)
        }
    }
}
