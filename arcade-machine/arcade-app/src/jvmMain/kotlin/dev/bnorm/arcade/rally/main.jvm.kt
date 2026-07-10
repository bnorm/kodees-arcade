@file:OptIn(ExperimentalMaterial3Api::class)

package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.display.GameViewModel
import dev.bnorm.arcade.display.InstallMenuItems
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.machine.Game
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

    val gameViewFactory: GameViewModel

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
        val appGraph = createGraphFactory<AppGraph.Factory>()
            .create(scope)

        Window(
            title = "Rally",
            state = rememberWindowState(width = 800.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            val windowGraph = appGraph.windowGraphFactory.create(this@Window)

            InstallMenuItems(windowGraph.items)

            var complete by remember { mutableStateOf<Game.Event.Complete?>(null) }
            complete?.let {
                BasicAlertDialog(
                    onDismissRequest = { complete = null },
                ) {
                    Surface {
                        RaceResults(it)
                    }
                }
            }

            val gameViewModel = appGraph.gameViewFactory
            Game(
                gameViewModel = gameViewModel,
                onComplete = {
                    complete = it
                    gameViewModel.clear()
                },
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}
