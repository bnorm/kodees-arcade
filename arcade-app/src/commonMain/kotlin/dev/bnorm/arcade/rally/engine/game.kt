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
                    RallyRacerState(
                        image = carImages.removeFirst(),
                        x = position.location.x,
                        y = position.location.y,
                        heading = position.heading,
                    )
                )
            }
        }
    )

    withEngine { engine ->
        val racers = racers.map { racer ->
            val racerState = gameState.racers.getValue(racer.name)
            engine.createWasmRacer(racerState, racer.bytes, racer.name) to racerState
        }

        for ((racer, _) in racers) {
            racer.onRace(track)
        }

        send(gameState)

        while (!gameState.finished) {
            // Allow racers to manipulate controls.
            for ((racer, racerState) in racers) {
                racer.move(gameState, racerState)
            }

            // Update game state.
            update(gameState, track)
            send(gameState)
        }
    }
}
