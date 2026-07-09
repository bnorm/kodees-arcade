package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.Track
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameViewModel(
    private val initialTrack: Track,
    scope: CoroutineScope,
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
}

data class GameModel(
    val active: Boolean,
    val running: Boolean,
    val desiredUps: Float,
    val actualUps: Float,
    val start: Game.Event.Start,
    val update: Game.Event.Update?,
)

@Composable
fun GamePresenter(
    initialTrack: Track,
    events: Flow<GameViewEvent>,
): GameModel {
    var running by remember { mutableStateOf(true) }
    var desiredUps by remember { mutableFloatStateOf(60f) }

    var game by remember { mutableStateOf<Game?>(null) }
    var start by remember {
        mutableStateOf(Game.Event.Start(initialTrack, emptyList()))
    }
    var update by remember(game) { mutableStateOf<Game.Event.Update?>(null) }

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
                    running = !running
                }

                is GameViewEvent.Ups -> {
                    desiredUps = it.ups
                }
            }
        }
    }

    LaunchedEffect(game) {
        // TODO automatically start?
        game?.start()
    }

    LaunchedEffect(game, running, desiredUps) {
        if (!running) return@LaunchedEffect
        val gameEvents = game?.events ?: return@LaunchedEffect

        withContext(Dispatchers.Default) {
            val frameDelay = (1.0 / desiredUps).seconds
            val startTime = TimeSource.Monotonic.markNow()
            var targetTime = 0.seconds

            for (event in gameEvents) {
                val currentTime = startTime.elapsedNow()
                val delay = targetTime - currentTime
                if (delay > Duration.ZERO) delay(delay)
                targetTime = maxOf(targetTime + frameDelay, currentTime)

                when (event) {
                    is Game.Event.Complete -> {
//                        onComplete(event)
                        update = null
                        break
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
        running = running,
        desiredUps = desiredUps,
        actualUps = desiredUps,
        start = start,
        update = update,
    )
}
