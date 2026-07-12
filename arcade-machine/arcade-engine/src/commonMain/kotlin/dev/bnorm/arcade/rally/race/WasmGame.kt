package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.machine.Game
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.DriverControlState
import dev.bnorm.arcade.rally.engine.RallyCarState
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.update
import dev.bnorm.arcade.rally.engine.wasm.createWasmDriver
import dev.bnorm.arcade.rally.engine.wasm.withEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import dev.bnorm.arcade.rally.Race as DriverRaceModel

class WasmGame(
    private val track: Track,
    private val drivers: List<WasmDriver>,
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

        onEvent(
            Game.Event.Update(
                drivers = gameState.drivers.map {
                    it.toDriver()
                },
            )
        )

        withEngine { engine ->
            val drivers = coroutineScope {
                drivers.mapIndexed { index, driver ->
                    async { engine.createWasmDriver(gameState.drivers[index].controls, driver.bytes, driver.name) }
                }
            }.awaitAll()


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

                    onEvent(
                        Game.Event.Update(
                            drivers = gameState.drivers.mapIndexed { index, state ->
                                val debug = if (state.name !in debug) {
                                    null
                                } else {
                                    val driver = drivers[index]
                                    driver.onDraw()
                                    Game.Event.Update.Driver.Debug(
                                        stdout = emptyList(), // TODO gather stdout as well
                                        canvasRequests = driver.canvasRequestBuffer.toList(),
                                    )
                                }

                                state.toDriver(debug)
                            },
                        )
                    )
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
            onEvent(Game.Event.Complete(results))
        }
    }
}

fun RallyCarState.toDriver(debug: Game.Event.Update.Driver.Debug? = null): Game.Event.Update.Driver {
    return Game.Event.Update.Driver(
        x = x,
        y = y,
        heading = heading,
        debug = debug,
    )
}

