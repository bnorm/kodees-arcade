package dev.bnorm.arcade.web

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation3.runtime.NavBackStack
import dev.bnorm.arcade.display.asset.font.Inter
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.web.route.BaseRouteKey
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import kotlinx.browser.window

@DependencyGraph(AppScope::class)
interface WebGraph {
    @Provides
    @SingleIn(AppScope::class)
    private fun provideClient(): ArcadeClient {
        val hostname = window.location.hostname
        val port = window.location.port.toIntOrNull()
        val secure = window.location.protocol == "https:"
        return ArcadeClient(host = hostname, port = port, secure = secure)
    }

    @Provides
    @SingleIn(AppScope::class)
    private fun provideBackStack(baseRouteKeys: Set<BaseRouteKey>): NavBackStack<RouteKey> {
        return NavBackStack(baseRouteKeys.route(window.location.hash))
    }

    val appScreen: AppScreen
}

fun main() {
    // TODO switch to Compose HTML?
    val graph = createGraph<WebGraph>()
    ComposeViewport("composeApp") {
        MaterialTheme(
            colorScheme = lightColorScheme().run {
                copy(

                )
            },
            shapes = Shapes().run {
                copy(

                )
            },
            typography = Typography().run {
                val fontFamily = Inter
                remember(this, fontFamily) {
                    copy(
                        displayLarge = displayLarge.copy(fontFamily = fontFamily),
                        displayMedium = displayMedium.copy(fontFamily = fontFamily),
                        displaySmall = displaySmall.copy(fontFamily = fontFamily),
                        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
                        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
                        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
                        titleLarge = titleLarge.copy(fontFamily = fontFamily),
                        titleMedium = titleMedium.copy(fontFamily = fontFamily),
                        titleSmall = titleSmall.copy(fontFamily = fontFamily),
                        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
                        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
                        bodySmall = bodySmall.copy(fontFamily = fontFamily),
                        labelLarge = labelLarge.copy(fontFamily = fontFamily),
                        labelMedium = labelMedium.copy(fontFamily = fontFamily),
                        labelSmall = labelSmall.copy(fontFamily = fontFamily),
                    )
                }
            }
        ) {
            graph.appScreen.Content()
        }
    }
}


