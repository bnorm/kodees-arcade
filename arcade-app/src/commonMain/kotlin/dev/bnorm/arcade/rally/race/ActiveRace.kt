package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.RacerControlState
import dev.bnorm.arcade.rally.engine.RallyCarState
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.update
import dev.bnorm.arcade.rally.engine.wasm.createWasmRacer
import dev.bnorm.arcade.rally.engine.wasm.withEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ActiveRace(
    private val track: Track,
    private val racers: List<Racer>,
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
                racers = buildMap {
                    for ((index, racer) in racers.withIndex()) {
                        val position = track.positions[index]
                        put(
                            racer.name,
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
                val controls = racers.associate { it.name to RacerControlState() }
                val racers = racers.map { racer ->
                    val controlsState = controls.getValue(racer.name)
                    engine.createWasmRacer(controlsState, racer.bytes, racer.name)
                }

                for (racer in racers) {
                    racer.onRace(track)
                }

                while (!gameState.finished) {
                    // Allow racers to manipulate controls.
                    for (racer in racers) {
                        // TODO stop calling when racer is finished.
                        //  - should they be removed from the game entirely when they finish?
                        racer.move(gameState)
                    }

                    // Update game state.
                    update(gameState, controls, track)
                    events.send(gameState.toUpdate())
                }

                events.send(Race.Event.Complete)
            }
        } finally {
            events.close()
        }
    }
}

private fun RallyGameState.toUpdate(): Race.Event {
    fun RallyCarState.toRacer(): Race.Event.Update.Racer {
        return Race.Event.Update.Racer(
            x = x,
            y = y,
            heading = heading
        )
    }

    return Race.Event.Update(
        time = time,
        racers = racers.mapValues { (_, value) -> value.toRacer() },
    )
}
