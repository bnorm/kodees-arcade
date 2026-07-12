package dev.bnorm.arcade.rally

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.Cache
import dev.bnorm.arcade.FileSystemCache
import dev.bnorm.arcade.NestedCache
import dev.bnorm.arcade.SerializedStringCache
import dev.bnorm.arcade.display.InstallMenuItems
import dev.bnorm.arcade.display.MenuItem
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.track.TrackViewModel.Companion.INITIAL_TRACK
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@DependencyGraph(AppScope::class)
interface AppGraph {
    val windowGraphFactory: WindowGraph.Factory

    val gameScreen: GameScreen

    @SingleIn(AppScope::class)
    @Provides
    fun provideArcadeClient(): ArcadeClient {
        return ArcadeClient()
    }

    @SingleIn(AppScope::class)
    @Provides
    fun providesCache(): Cache<String> {
        return FileSystemCache(directory = Paths.get(".arcade").createDirectories())
    }

    @SingleIn(AppScope::class)
    @Provides
    fun providesTrackCache(cache: Cache<String>): Cache<Track> {
        val cache = SerializedStringCache(
            delegate = NestedCache(cache, part = "track"),
            serializer = Track.serializer(),
            format = Json,
        )
        if (cache.keys.isEmpty()) {
            cache["initial"] = INITIAL_TRACK
        }
        return cache
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
            state = rememberWindowState(width = 1000.dp, height = 1000.dp),
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
