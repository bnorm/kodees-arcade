package dev.bnorm.arcade.rally.sample

import dev.bnorm.arcade.rally.racerMove
import dev.bnorm.arcade.rally.racerOnRace

private val racer = Kodee

/**
 * Wasm exported function used by the game engine to call our racer.
 */
// TODO can this be auto-generated somehow?
//  special Gradle plugin to handle this?
//  will the component model make all of this obsolete?
@OptIn(ExperimentalWasmInterop::class)
@WasmExport
fun onRace() {
    racerOnRace(racer)
}

/**
 * Wasm exported function used by the game engine to call our racer.
 */
// TODO can this be auto-generated somehow?
//  special Gradle plugin to handle this?
//  will the component model make all of this obsolete?
@OptIn(ExperimentalWasmInterop::class)
@WasmExport
fun move() = racerMove(racer)
