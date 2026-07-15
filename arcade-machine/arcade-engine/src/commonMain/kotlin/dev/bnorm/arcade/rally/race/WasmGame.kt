package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.engine.RallyCarState
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.update
import dev.bnorm.arcade.rally.engine.wasm.WasmDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.io.buffered
import kotlinx.io.readString
import dev.bnorm.arcade.driver.Race as DriverRaceModel

class WasmGame(
    private val track: Track,
    private val drivers: List<Driver>,
    private val laps: Int,
) : Game {
    init {
        require(drivers.size <= track.positions.size)
    }

    private val debug = mutableSetOf<String>()
    override fun setDebug(driver: String, debug: Boolean) {
        if (debug) this.debug += driver else this.debug -= driver
    }

    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        val raceModel = DriverRaceModel(track, laps)

        onEvent(Game.Event.Start(track, drivers.map { it.name }))

        val gameState = RallyGameState(
            trackWidth = track.width,
            trackHeight = track.height,
            laps = laps,
            finished = false,
            time = 0,
            driverStates = coroutineScope {
                List(drivers.size) {
                    async {
                        val driver = drivers[it]
                        val position = track.positions[it]
                        RallyCarState(
                            name = driver.name,
                            driver = WasmDriver(driver.name, driver.bytes),
                            x = position.location.x,
                            y = position.location.y,
                            heading = position.heading,
                        )
                    }
                }
            }.awaitAll()
        )

        try {
            for (state in gameState.driverStates) {
                state.driver.onRace(raceModel)
            }

            onEvent(
                Game.Event.Update(
                    drivers = gameState.driverStates.map { it.toDriverUpdate() },
                )
            )

            while (!gameState.finished) {
                // Allow drivers to manipulate controls.
                for (state in gameState.driverStates) {
                    if (state.finished) continue

                    state.driver.move(
                        Car(
                            time = gameState.time,
                            location = Point(state.x, state.y),
                            velocity = Vector(state.heading, state.speed),
                            lap = state.lap,
                            nextCheckpoint = state.checkpoint,
                        )
                    )
                }

                // Update game state.
                update(gameState, track)

                onEvent(
                    Game.Event.Update(
                        drivers = gameState.driverStates.map { state ->
                            state.toDriverUpdate(
                                Game.Event.Update.Driver.Debug(
                                    stdout = state.driver.stdout.buffered().readString().takeIf { it.isNotEmpty() },
                                    stderr = state.driver.stderr.buffered().readString().takeIf { it.isNotEmpty() },
                                    drawRequests = state.takeIf { state.name in debug }
                                        ?.driver?.onDraw().orEmpty(),
                                )
                            )
                        },
                    )
                )
            }
        } finally {
            for (state in gameState.driverStates) {
                state.driver.close()
            }
        }

        val results = gameState.driverStates
            .associate { state -> state.name to Game.Event.Complete.Result(state.lapTimes) }
        onEvent(Game.Event.Complete(results))
    }
}

private fun RallyCarState.toDriverUpdate(debug: Game.Event.Update.Driver.Debug? = null): Game.Event.Update.Driver {
    return Game.Event.Update.Driver(
        x = x,
        y = y,
        heading = heading,
        debug = debug,
    )
}

