package dev.bnorm.arcade.app

import dev.bnorm.arcade.Cache
import dev.bnorm.arcade.NestedCache
import dev.bnorm.arcade.SerializedStringCache
import dev.bnorm.arcade.StorageCache
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.WebMenu
import dev.bnorm.arcade.display.game.GameScreen
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@DependencyGraph(AppScope::class)
interface AppGraph {
    val gameScreen: GameScreen
    val webMenu: WebMenu

    @SingleIn(AppScope::class)
    @Provides
    fun provideArcadeClient(): ArcadeClient? {
        val hostname = window.location.hostname
        val port = window.location.port.toIntOrNull() ?: 8080
        return if (hostname == "localhost") {
            ArcadeClient(host = hostname, port = port)
        } else {
            null
        }
    }

    @SingleIn(AppScope::class)
    @Provides
    fun providesCache(): Cache<String> {
        return StorageCache(localStorage)
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
