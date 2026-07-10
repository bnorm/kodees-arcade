package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.Race as DriverRaceModel
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.DriverControlState
import dev.bnorm.arcade.rally.engine.RallyCarState
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.update
import dev.bnorm.arcade.rally.engine.wasm.createWasmDriver
import dev.bnorm.arcade.rally.engine.wasm.withEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class WasmGame(
    private val track: Track,
    private val drivers: List<WasmDriver>,
    private val laps: Int,
) : Game {
    init {
        require(drivers.size <= track.positions.size)
    }

    override val events: ReceiveChannel<Game.Event>
        field = Channel()

    override suspend fun start() {
        val raceModel = DriverRaceModel(track, laps)

        try {
            events.send(Game.Event.Start(track, drivers.map { it.name }))

            val gameState = RallyGameState(
                trackWidth = track.width,
                trackHeight = track.height,
                laps = laps,
                finished = false,
                time = 0,
                drivers = List(drivers.size) {
                    val position = track.positions[it]
                    RallyCarState(
                        name = drivers[it].name,
                        controls = DriverControlState(),
                        x = position.location.x,
                        y = position.location.y,
                        heading = position.heading,
                    )
                }
            )

            events.send(gameState.toUpdate())

            withEngine { engine ->
                val drivers = drivers.mapIndexed { index, driver ->
                    engine.createWasmDriver(gameState.drivers[index].controls, driver.bytes, driver.name)
                }

                try {
                    for (driver in drivers) {
                        driver.onRace(raceModel)
                    }

                    while (!gameState.finished) {
                        // Allow drivers to manipulate controls.
                        repeat(drivers.size) { index ->
                            // TODO stop calling when driver is finished.
                            //  - should they be removed from the game entirely when they finish?

                            val carState = gameState.drivers[index]
                            val car = Car(
                                time = gameState.time,
                                location = Point(carState.x, carState.y),
                                velocity = Vector(carState.heading, carState.speed),
                                lap = carState.lap,
                                nextCheckpoint = carState.checkpoint,
                            )

                            drivers[index].move(car)
                        }

                        // Update game state.
                        update(gameState, track)
                        events.send(gameState.toUpdate())
                    }
                } finally {
                    for (driver in drivers) {
                        driver.close()
                    }
                }

                val results = gameState.drivers
                    .sortedBy { it.finished }.withIndex()
                    .associate { (place, state) ->
                        state.name to Game.Event.Complete.Result(place + 1, state.finished!!)
                    }
                events.send(Game.Event.Complete(results))
            }
        } catch (t: Throwable) {
            events.close(t)
        } finally {
            events.close()
        }
    }
}

private fun RallyGameState.toUpdate(): Game.Event {
    fun RallyCarState.toDriver(): Game.Event.Update.Driver {
        return Game.Event.Update.Driver(
            x = x,
            y = y,
            heading = heading
        )
    }

    return Game.Event.Update(
        drivers = drivers.map { it.toDriver() },
    )
}
