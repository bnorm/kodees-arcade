@file:OptIn(UnsafeWasmMemoryApi::class)

package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.rally.Canvas
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Controls
import dev.bnorm.arcade.rally.Driver
import dev.bnorm.arcade.rally.Race
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is designed only for exporting functions to the Wasm module."
)
annotation class DriverExport

/**
 * Helper function to use Wasm memory for a [Race].
 */
@DriverExport
fun driverOnRace(driver: Driver) {
    try {
        val race = Pointer(0u).loadProtoBuf(Race.serializer())
        driver.onRace(race)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Car] and [Controls].
 */
@DriverExport
fun driverMove(driver: Driver) {
    try {
        val car = Pointer(0u).loadProtoBuf(Car.serializer())
        driver.move(car, ImportControls)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Canvas].
 */
@DriverExport
fun driverOnDraw(driver: Driver) {
    try {
        driver.onDraw(ImportCanvas)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}
