package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.engine.RacerControlState
import dev.bnorm.arcade.rally.engine.RallyGameState

expect suspend fun WasmEngine.createWasmRacer(
    controlState: RacerControlState,
    racer: ByteArray,
    name: String,
): WasmRacer

class WasmRacer(
    private val name: String,
    private val memory: WasmMemory,
    private val moveFunction: () -> Unit,
    private val onRaceFunction: () -> Unit,
    private val onClose: () -> Unit,
) : AutoCloseable {
    fun move(gameState: RallyGameState) {
        val carState = gameState.racers.getValue(name)
        val car = Car(
            time = gameState.time,
            location = Point(carState.x, carState.y),
            velocity = Vector(carState.heading, carState.speed),
            lap = carState.lap,
            nextCheckpoint = carState.checkpoint,
        )

        memory.writeProto(0, Car.serializer(), car)
        moveFunction.invoke()
    }


    fun onRace(track: Track) {
        memory.writeProto(0, Track.serializer(), track)
        onRaceFunction.invoke()
    }

    override fun close() {
        onClose.invoke()
    }
}
