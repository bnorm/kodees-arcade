package dev.bnorm.arcade.web

import androidx.compose.ui.window.ComposeViewport
import app.softwork.routingcompose.HashRouter
import app.softwork.routingcompose.Router
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.web.route.WebRouter
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
        val secure = window.location.protocol == "https:"
        return ArcadeClient(host = hostname, port = port, secure = secure)
    }

    @Multibinds
    val webRouters: Set<WebRouter>
}

fun main() {
    val graph = createGraph<WebGraph>()

    // TODO switch to Compose HTML?
    ComposeViewport("composeApp") {
        HashRouter(initPath = "/") {
            for (router in graph.webRouters) {
                with(router) {
                    Route()
                }
            }
            noMatch {
                Router.current.navigate(to = "/", replace = true)
            }
        }
    }
}
