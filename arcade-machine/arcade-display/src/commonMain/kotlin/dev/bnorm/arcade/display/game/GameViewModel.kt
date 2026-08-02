package dev.bnorm.arcade.display.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bnorm.arcade.display.ViewModel
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.game.driver.DriverDebugViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.machine.Game
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Inject
class GameViewModel(
    private val initialTrack: Track = TrackViewModel.INITIAL_TRACK,
    @ViewModelCoroutineScope scope: CoroutineScope,
    private val driverDebugViewModel: DriverDebugViewModel,
) : ViewModel<GameViewEvent, GameModel>(scope) {
    fun clear() {
        take(GameViewEvent.Clear)
    }

    fun new(game: Game) {
        // TODO should we clear?
        // TODO is this best place to clear?
        driverDebugViewModel.clear()
        take(GameViewEvent.New(game))
    }

    fun restart() {
        take(GameViewEvent.Restart)
    }

    fun stop() {
        take(GameViewEvent.Clear)
    }

    fun togglePlayPause() {
        take(GameViewEvent.TogglePlayPause)
    }

    fun step() {
        take(GameViewEvent.Step)
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
    data object Restart : GameViewEvent()
    data class New(val game: Game) : GameViewEvent()

    data object TogglePlayPause : GameViewEvent()
    data object Step : GameViewEvent()
    data class Ups(val ups: Float) : GameViewEvent()
}

data class GameModel(
    val active: Boolean,
    val running: Boolean,
    val desiredUps: Float,
    val actualUps: Float,
    val start: Game.Event.Start,
    val update: Game.Event.Update?,
    val complete: Game.Event.Complete?,
    val driverOutput: Map<String, TextLines>
)

@Composable
fun GamePresenter(
    initialTrack: Track,
    events: Flow<GameViewEvent>,
): GameModel {
    var game by remember { mutableStateOf<Game?>(null) }
    var active by remember { mutableStateOf(false) }

    val ticker = remember { Channel<Unit>() }
    var desiredUps by remember { mutableFloatStateOf(60f) }
    var running by remember { mutableStateOf(true) }

    var start by remember { mutableStateOf(Game.Event.Start(initialTrack, emptyList())) }
    var update by remember { mutableStateOf<Game.Event.Update?>(null) }
    var complete by remember { mutableStateOf<Game.Event.Complete?>(null) }

    val driverOutput = remember { mutableStateMapOf<String, TextLines>() }

    LaunchedEffect(events) {
        events.collect {
            when (it) {
                GameViewEvent.Clear -> {
                    active = false
                    update = null
                    complete = null
                }

                is GameViewEvent.New -> {
                    game = it.game
                    active = true
                    update = null
                    complete = null
                }

                GameViewEvent.Restart -> {
                    active = true
                    update = null
                    complete = null
                }

                GameViewEvent.TogglePlayPause -> {
                    running = !running
                }

                GameViewEvent.Step -> {
                    ticker.trySend(Unit)
                }

                is GameViewEvent.Ups -> {
                    desiredUps = it.ups
                }
            }
        }
    }

    // Start the game and consume events.
    LaunchedEffect(active, game) {
        if (!active) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            game?.start { event ->
                when (event) {
                    is Game.Event.Complete -> {
                        complete = event
                    }

                    is Game.Event.Start -> {
                        start = event
                        driverOutput.clear()
                        for (name in event.drivers) {
                            driverOutput[name] = TextLines()
                        }
                    }

                    is Game.Event.Update -> {
                        update = event
                        for ((index, driver) in event.drivers.withIndex()) {
                            val stdout = driver.debug?.stdout
                            val stderr = driver.debug?.stderr
                            if (stdout == null && stderr == null) continue

                            val lines = driverOutput.getValue(start.drivers[index])
                            if (stdout != null) lines.append(stdout)
                            if (stderr != null) lines.append(stderr)
                        }
                    }
                }

                // This must be at the end!
                // The ticker controls when the game engine can *compute* the next event,
                // not when we can *consume* the next game engine event.
                // This makes sure debug mode can be enabled while paused,
                // and we will see debug information on the very next tick.
                ticker.receive()
            }
            active = false
        }
    }

    // Launch a "ticker", that controls how quickly events are computed by the game.
    LaunchedEffect(active, game, ticker, running) {
        withContext(Dispatchers.Default) {
            if (active && running && game != null) {
                val startTime = TimeSource.Monotonic.markNow()
                var targetTime = 0.seconds

                while (true) {
                    val currentTime = startTime.elapsedNow()
                    val delay = targetTime - currentTime
                    if (delay > Duration.ZERO) delay(delay)
                    targetTime = maxOf(targetTime + (1.0 / desiredUps).seconds, currentTime)
                    ticker.send(Unit)
                }
            }
        }
    }

    return GameModel(
        active = active,
        running = running,
        desiredUps = desiredUps,
        actualUps = desiredUps,
        start = start,
        update = update,
        complete = complete,
        driverOutput = driverOutput,
    )
}


