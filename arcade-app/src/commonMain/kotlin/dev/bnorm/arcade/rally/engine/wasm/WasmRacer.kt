package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.rally.engine.RallyGameState
import dev.bnorm.arcade.rally.engine.RallyRacerState

expect suspend fun WasmEngine.createWasmRacer(
    racerState: RallyRacerState,
    racer: ByteArray,
    name: String,
): WasmRacer

class WasmRacer(
    private val memory: WasmMemory,
    private val moveFunction: () -> Unit,
    private val onRaceFunction: () -> Unit,
) {
    fun move(gameState: RallyGameState, carState: RallyRacerState) {
        val car = Car(
            time = gameState.time,
            location = Point(carState.x, carState.y),
            velocity = Vector(carState.heading, carState.speed),
            nextCheckpoint = carState.checkpoint,
        )

        memory.writeProto(0, Car.serializer(), car)
        moveFunction.invoke()
    }


    fun onRace(track: Track) {
        memory.writeProto(0, Track.serializer(), track)
        onRaceFunction.invoke()
    }
}
