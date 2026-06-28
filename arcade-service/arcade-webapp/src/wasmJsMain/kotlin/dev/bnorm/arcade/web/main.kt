package dev.bnorm.arcade.web

import androidx.compose.ui.window.ComposeViewport
import app.softwork.routingcompose.HashRouter
import dev.bnorm.arcade.route.Route
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
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
        return ArcadeClient(host = hostname, port = port)
    }

    @Multibinds
    val routes: Set<Route>
}

fun main() {
    val graph = createGraph<WebGraph>()

    // TODO switch to Compose HTML?
    ComposeViewport("composeApp") {
        HashRouter(initPath = "/") {
            for (route in graph.routes) {
                route(route.path) {
                    route.Content()
                }
            }
        }
    }
}

