@file:OptIn(UnsafeWasmMemoryApi::class)

package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Controls
import dev.bnorm.arcade.rally.Racer
import dev.bnorm.arcade.rally.Track
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is designed only for exporting functions to the Wasm module."
)
annotation class RacerExport

/**
 * Helper function to use Wasm memory for a [Track].
 */
@RacerExport
fun racerOnRace(racer: Racer) {
    try {
        val track = Pointer(0u).loadProtoBuf(Track.serializer())
        racer.onRace(track)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Car] and [Controls].
 */
@RacerExport
fun racerMove(racer: Racer) {
    try {
        val car = Pointer(0u).loadProtoBuf(Car.serializer())
        racer.move(car, ImportControls)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}
