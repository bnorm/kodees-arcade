package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import kotlinx.io.RawSource

interface WasmModule {
    suspend fun createDriver(name: String): WasmDriver
}

expect suspend fun WasmModule(bytes: ByteArray): WasmModule

interface WasmDriver : AutoCloseable {
    val name: String

    val steering: Double
    val throttle: Double

    val stdout: RawSource
    val stderr: RawSource

    fun onRace(race: Race)
    fun onTurn(car: Car)
    fun onCar(car: Car)
    fun onDraw(): List<DrawRequest>
}
