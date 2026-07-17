package dev.bnorm.arcade.web

import androidx.compose.ui.window.ComposeViewport
import androidx.navigation3.runtime.NavBackStack
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.restoreKey
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
    private fun provideBackStack(): NavBackStack<RouteKey> {
        return NavBackStack(restoreKey(window.location.hash))
    }

    val appScreen: AppScreen
}

fun main() {
    // TODO switch to Compose HTML?
    val graph = createGraph<WebGraph>()
    ComposeViewport("composeApp") {
        graph.appScreen.Content()
    }
}
