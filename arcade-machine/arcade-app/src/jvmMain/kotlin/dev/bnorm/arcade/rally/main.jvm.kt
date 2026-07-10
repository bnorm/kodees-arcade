package dev.bnorm.arcade.rally

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.InstallMenuItems
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(AppScope::class)
interface AppGraph {
    val windowGraphFactory: WindowGraph.Factory

    val gameScreen: GameScreen

    @SingleIn(AppScope::class)
    @Provides
    fun provideArcadeClient(): ArcadeClient {
        return ArcadeClient()
    }

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides @ViewModelCoroutineScope scope: CoroutineScope,
        ): AppGraph
    }
}

@GraphExtension(WindowScope::class)
interface WindowGraph {
    val items: Set<MenuItem>

    @GraphExtension.Factory
    interface Factory {
        fun create(
            @Provides windowScope: WindowScope
        ): WindowGraph
    }
}

fun main() {
    application {
        val scope = rememberCoroutineScope()
        val appGraph = remember(scope) {
            createGraphFactory<AppGraph.Factory>()
                .create(scope)
        }

        Window(
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            val windowGraph = remember(appGraph) {
                appGraph.windowGraphFactory
                    .create(this@Window)
            }

            InstallMenuItems(windowGraph.items)
            appGraph.gameScreen.Content()
        }
    }
}
