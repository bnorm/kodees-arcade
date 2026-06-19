private val racer = dev.bnorm.arcade.rally.sample.Kodee

/**
 * Wasm exported function used by the game engine to call our racer.
 */
// TODO can this be auto-generated somehow?
//  special Gradle plugin to handle this?
//  will the component model make all of this obsolete?
@OptIn(ExperimentalWasmInterop::class)
@WasmExport
fun onRace() {
    dev.bnorm.arcade.rally.racerOnRace(racer)
}

/**
 * Wasm exported function used by the game engine to call our racer.
 */
// TODO can this be auto-generated somehow?
//  special Gradle plugin to handle this?
//  will the component model make all of this obsolete?
@OptIn(ExperimentalWasmInterop::class)
@WasmExport
fun move() {
    dev.bnorm.arcade.rally.racerMove(racer)
}
