package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.moveRacer

/**
 * Wasm exported function used by the game engine to call our racer.
 */
// TODO can this be auto-generated somehow?
//  will the component model make all of this obsolete?
@OptIn(ExperimentalWasmInterop::class)
@WasmExport
fun move() = moveRacer(Kodee)
