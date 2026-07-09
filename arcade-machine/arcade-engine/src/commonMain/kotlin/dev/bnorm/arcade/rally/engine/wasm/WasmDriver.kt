package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Race
import dev.bnorm.arcade.rally.engine.DriverControlState

expect suspend fun WasmEngine.createWasmDriver(
    controlState: DriverControlState,
    driver: ByteArray,
    name: String,
): WasmDriver

class WasmDriver(
    private val memory: WasmMemory,
    private val moveFunction: () -> Unit,
    private val onRaceFunction: () -> Unit,
    private val onClose: () -> Unit,
) : AutoCloseable {
    fun move(car: Car) {
        memory.writeProto(0, Car.serializer(), car)
        moveFunction.invoke()
    }

    fun onRace(race: Race) {
        memory.writeProto(0, Race.serializer(), race)
        onRaceFunction.invoke()
    }

    override fun close() {
        onClose.invoke()
    }
}
