package dev.bnorm.arcade.rally

import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

abstract class Racer {
    abstract fun move(car: Car, controls: Controls)
}

/**
 * Helper function to help translate WASM memory and use imported host functions.
 */
@OptIn(UnsafeWasmMemoryApi::class)
fun moveRacer(racer: Racer) {
    racer.move(MemoryCar(Pointer(0u)), WasmImportControls)
}
