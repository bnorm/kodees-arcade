package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.machine.Game
import kotlinx.io.buffered
import kotlinx.io.readString
import dev.bnorm.arcade.driver.Race as DriverRaceModel

class WasmGame(
    private val track: Track,
    private val drivers: List<WasmDriver>,
    private val laps: Int,
    private val driverDebug: Game.DriverDebug = Game.DriverDebug.Disabled,
) : Game {
    init {
        require(drivers.size <= track.positions.size)
    }

    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        val raceModel = DriverRaceModel(track, laps)

        onEvent(Game.Event.Start(track, drivers.map { it.name }))

        val gameState = GameState(
            track = track,
            laps = laps,
            finished = false,
            time = 0,
            driverStates = List(drivers.size) {
                val driver = drivers[it]
                val position = track.positions[it]
                DriverState(
                    driver = driver,
                    x = position.location.x,
                    y = position.location.y,
                    heading = position.heading,
                )
            }
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

                    state.driver.onTurn(
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
                update(gameState)

                onEvent(
                    Game.Event.Update(
                        drivers = gameState.driverStates.map { state ->
                            state.toDriverUpdate(
                                Game.Event.Update.Driver.Debug(
                                    stdout = state.driver.stdout.buffered().readString().takeIf { it.isNotEmpty() },
                                    stderr = state.driver.stderr.buffered().readString().takeIf { it.isNotEmpty() },
                                    drawRequests = when (driverDebug.isEnabled(state.driver.name)) {
                                        true -> state.driver.onDraw()
                                        false -> emptyList()
                                    },
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
            .associate { state -> state.driver.name to Game.Event.Complete.Result(state.lapTimes) }
        onEvent(Game.Event.Complete(results))
    }
}

private fun DriverState.toDriverUpdate(debug: Game.Event.Update.Driver.Debug? = null): Game.Event.Update.Driver {
    return Game.Event.Update.Driver(
        x = x,
        y = y,
        heading = heading,
        debug = debug,
    )
}

