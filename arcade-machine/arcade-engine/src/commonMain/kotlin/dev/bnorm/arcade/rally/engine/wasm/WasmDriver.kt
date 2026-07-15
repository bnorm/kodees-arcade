package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import kotlinx.io.RawSource

expect suspend fun WasmDriver(name: String, bytes: ByteArray): WasmDriver

interface WasmDriver : AutoCloseable {
    val steering: Double
    val throttle: Double

    val stdout: RawSource
    val stderr: RawSource

    fun move(car: Car)
    fun onRace(race: Race)
    fun onDraw(): List<DrawRequest>
}
