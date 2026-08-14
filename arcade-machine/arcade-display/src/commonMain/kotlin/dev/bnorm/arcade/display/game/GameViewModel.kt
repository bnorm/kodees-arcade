package dev.bnorm.arcade.display.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bnorm.arcade.display.ViewModel
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.display.game.driver.DriverDebugViewModel
import dev.bnorm.arcade.display.track.TrackViewModel
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.geometry.Angle
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

@Stable
data class GameModel(
    val active: Boolean,
    val running: Boolean,
    val desiredUps: Float,
    val actualUps: Float,
    val track: Track,
    val drivers: List<DriverState>,
    val complete: Game.Event.Complete?,
) {
    @Stable
    data class DriverState(
        // Display
        val name: String,
        val x: Double,
        val y: Double,
        val heading: Angle,
        // Progress
        val lap: Int = 0,
        val checkpoint: Int = 0,
        // Debug
        val drawRequests: List<DrawRequest> = emptyList(),
        val output: TextLines = TextLines()
    )
}

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

    var track by remember { mutableStateOf(initialTrack) }
    val drivers = remember { mutableStateListOf<GameModel.DriverState>() }
    var complete by remember { mutableStateOf<Game.Event.Complete?>(null) }

    LaunchedEffect(events) {
        events.collect {
            when (it) {
                GameViewEvent.Clear -> {
                    active = false
                    drivers.clear()
                    complete = null
                }

                is GameViewEvent.New -> {
                    game = it.game
                    active = true
                    drivers.clear()
                    complete = null
                }

                GameViewEvent.Restart -> {
                    active = true
                    drivers.clear()
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
                        track = event.track
                        drivers.clear()
                        for ((index, name) in event.drivers.withIndex()) {
                            val position = event.track.positions[index]
                            drivers.add(
                                GameModel.DriverState(
                                    name = name,
                                    x = position.location.x,
                                    y = position.location.y,
                                    heading = position.heading,
                                )
                            )
                        }
                    }

                    is Game.Event.Update -> {
                        for ((index, update) in event.drivers.withIndex()) {
                            val driver = drivers[index].copy(
                                x = update.x,
                                y = update.y,
                                heading = update.heading,
                                drawRequests = update.debug?.drawRequests.orEmpty()
                            )
                            update.debug?.stdout?.let { driver.output.append(it) }
                            update.debug?.stderr?.let { driver.output.append(it) }
                            drivers[index] = driver
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
        track = track,
        drivers = drivers.toList(),
        complete = complete,
    )
}
