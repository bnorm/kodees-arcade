package dev.bnorm.arcade.display.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bnorm.arcade.display.ViewModel
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.Track
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Inject
class GameViewModel(
    private val initialTrack: Track = TrackViewModel.INITIAL_TRACK,
    @ViewModelCoroutineScope scope: CoroutineScope,
) : ViewModel<GameViewEvent, GameModel>(scope) {
    fun clear() {
        take(GameViewEvent.Clear)
    }

    fun new(game: Game) {
        take(GameViewEvent.New(game))
    }

    fun stop() {
        take(GameViewEvent.Clear)
    }

    fun togglePlayPause() {
        take(GameViewEvent.TogglePlayPause)
    }

    fun adjustUps(ups: Float) {
        take(GameViewEvent.Ups(ups))
    }

    fun setDebug(driver: String, debug: Boolean) {
        take(GameViewEvent.SetDebug(driver, debug))
    }

    @Composable
    override fun models(events: Flow<GameViewEvent>): GameModel {
        return GamePresenter(initialTrack, events)
    }
}

sealed class GameViewEvent {
    data object Clear : GameViewEvent()
    data class New(val game: Game) : GameViewEvent()

    data object TogglePlayPause : GameViewEvent()
    data class Ups(val ups: Float) : GameViewEvent()

    data class SetDebug(val driver: String, val debug: Boolean) : GameViewEvent()
}

data class GameModel(
    val active: Boolean,
    val running: Boolean,
    val desiredUps: Float,
    val actualUps: Float,
    val start: Game.Event.Start,
    val update: Game.Event.Update?,
    val complete: Game.Event.Complete?,
)

@Composable
fun GamePresenter(
    initialTrack: Track,
    events: Flow<GameViewEvent>,
): GameModel {
    var running by remember { mutableStateOf(CompletableDeferred(Unit)) }
    var desiredUps by remember { mutableFloatStateOf(60f) }

    var game by remember { mutableStateOf<Game?>(null) }
    var start by remember {
        mutableStateOf(Game.Event.Start(initialTrack, emptyList()))
    }
    var update by remember(game) { mutableStateOf<Game.Event.Update?>(null) }
    var complete by remember(game) { mutableStateOf<Game.Event.Complete?>(null) }

    LaunchedEffect(events) {
        events.collect {
            when (it) {
                GameViewEvent.Clear -> {
                    game = null
                }

                is GameViewEvent.New -> {
                    game = it.game
                }

                GameViewEvent.TogglePlayPause -> {
                    // TODO this is so ugly...
                    if (running.isCompleted) {
                        // Pause
                        running = CompletableDeferred()
                    } else {
                        // Play
                        running.complete(Unit)
                    }
                }

                is GameViewEvent.Ups -> {
                    desiredUps = it.ups
                }

                is GameViewEvent.SetDebug -> {
                    game?.setDebug(it.driver, it.debug)
                }
            }
        }
    }

    LaunchedEffect(game) {
        withContext(Dispatchers.Default) {
            val startTime = TimeSource.Monotonic.markNow()
            var targetTime = 0.seconds

            game?.start { event ->
                running.join()
                val currentTime = startTime.elapsedNow()
                val delay = targetTime - currentTime
                if (delay > Duration.ZERO) delay(delay)
                targetTime = maxOf(targetTime + (1.0 / desiredUps).seconds, currentTime)

                when (event) {
                    is Game.Event.Complete -> {
                        complete = event
                    }

                    is Game.Event.Start -> {
                        start = event
                    }

                    is Game.Event.Update -> {
                        update = event
                    }
                }
            }
        }
    }

    return GameModel(
        active = game != null,
        running = running.isCompleted,
        desiredUps = desiredUps,
        actualUps = desiredUps,
        start = start,
        update = update,
        complete = complete,
    )
}
