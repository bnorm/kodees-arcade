package dev.bnorm.arcade.rally.engine

import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.wasm.createWasmRacer
import dev.bnorm.arcade.rally.engine.wasm.withEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.game(
    track: Track,
    racers: List<Racer>,
    carImages: List<ImageBitmap>,
): ReceiveChannel<RallyGameState> = produce {
    val carImages = ArrayDeque(carImages.shuffled())

    var gameState = RallyGameState(
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
                        image = carImages.removeFirst(),
                        x = position.location.x,
                        y = position.location.y,
                        heading = position.heading,
                    )
                )
            }
        }
    )
    val controls = racers.associate { it.name to RacerControlState() }

    withEngine { engine ->
        val racers = racers.map { racer ->
            val controlsState = controls.getValue(racer.name)
            engine.createWasmRacer(controlsState, racer.bytes, racer.name)
        }

        for (racer in racers) {
            racer.onRace(track)
        }

        send(gameState)

        while (!gameState.finished) {
            // Allow racers to manipulate controls.
            for (racer in racers) {
                // TODO stop calling when racer is finished.
                //  - should they be removed from the game entirely when they finish?
                racer.move(gameState)
            }

            // Update game state.
            gameState = update(gameState, controls, track)
            send(gameState)
        }
    }
}
