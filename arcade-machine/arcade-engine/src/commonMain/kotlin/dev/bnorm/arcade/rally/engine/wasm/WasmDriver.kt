package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
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
    private val onDrawFunction: (() -> Unit)?,
    private val onClose: () -> Unit,
    private val drawRequests: MutableList<DrawRequest>
) : AutoCloseable {
    fun move(car: Car) {
        memory.writeProto(0, Car.serializer(), car)
        moveFunction.invoke()
    }

    fun onRace(race: Race) {
        memory.writeProto(0, Race.serializer(), race)
        onRaceFunction.invoke()
    }

    fun onDraw() {
        if (onDrawFunction != null) {
            drawRequests.clear()
            onDrawFunction()
        }
    }

    val canvasRequestBuffer: List<DrawRequest>
        get() = drawRequests

    override fun close() {
        onClose.invoke()
    }
}
