package dev.bnorm.arcade.app

import dev.bnorm.arcade.Cache
import dev.bnorm.arcade.FileSystemCache
import dev.bnorm.arcade.NestedCache
import dev.bnorm.arcade.SerializedStringCache
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
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
            format = Json.Default,
        )
        if (cache.keys.isEmpty()) {
            cache["initial"] = TrackViewModel.INITIAL_TRACK
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
