package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.machine.Race
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.DriverControlState
import dev.bnorm.arcade.rally.engine.RallyCarState
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.update
import dev.bnorm.arcade.rally.engine.wasm.createWasmDriver
import dev.bnorm.arcade.rally.engine.wasm.withEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class WasmRace(
    private val track: Track,
    private val drivers: List<WasmDriver>,
) : Race {
    override val events: ReceiveChannel<Race.Event>
        field = Channel()

    override suspend fun start() {
        try {
            events.send(Race.Event.Start(track))

            val gameState = RallyGameState(
                trackWidth = track.width,
                trackHeight = track.height,
                finished = false,
                time = 0,
                drivers = buildMap {
                    for ((index, driver) in drivers.withIndex()) {
                        val position = track.positions[index]
                        put(
                            driver.name,
                            RallyCarState(
                                x = position.location.x,
                                y = position.location.y,
                                heading = position.heading,
                            )
                        )
                    }
                }
            )

            events.send(gameState.toUpdate())

            withEngine { engine ->
                val controls = this@WasmRace.drivers.associate { it.name to DriverControlState() }
                val drivers = this@WasmRace.drivers.map { driver ->
                    val controlsState = controls.getValue(driver.name)
                    engine.createWasmDriver(controlsState, driver.bytes, driver.name)
                }

                try {
                    for (driver in drivers) {
                        driver.onRace(track)
                    }

                    while (!gameState.finished) {
                        // Allow drivers to manipulate controls.
                        for (driver in drivers) {
                            // TODO stop calling when driver is finished.
                            //  - should they be removed from the game entirely when they finish?
                            driver.move(gameState)
                        }

                        // Update game state.
                        update(gameState, controls, track)
                        events.send(gameState.toUpdate())
                    }
                } finally {
                    for (driver in drivers) {
                        driver.close()
                    }
                }

                val results = gameState.drivers.entries
                    .sortedBy { (_, v) -> v.finished }
                    .map { it.key }.withIndex()
                    .associate { (place, name) ->
                        name to Race.Event.Complete.Result(place + 1, gameState.drivers.getValue(name).finished!!)
                    }
                events.send(Race.Event.Complete(results))
            }
        } catch (t: Throwable) {
            events.close(t)
        } finally {
            events.close()
        }
    }
}

private fun RallyGameState.toUpdate(): Race.Event {
    fun RallyCarState.toDriver(): Race.Event.Update.Driver {
        return Race.Event.Update.Driver(
            x = x,
            y = y,
            heading = heading
        )
    }

    return Race.Event.Update(
        time = time,
        drivers = drivers.mapValues { (_, value) -> value.toDriver() },
    )
}
