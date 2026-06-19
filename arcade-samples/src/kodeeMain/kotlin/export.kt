@file:OptIn(dev.bnorm.arcade.rally.internal.RacerExport::class, ExperimentalWasmInterop::class)

private val racer = dev.bnorm.arcade.rally.sample.Kodee

/**
 * Wasm exported function used by the game engine to call our racer.
 */
@WasmExport
fun onRace() {
    dev.bnorm.arcade.rally.internal.racerOnRace(racer)
}

/**
 * Wasm exported function used by the game engine to call our racer.
 */
@WasmExport
fun move() {
    dev.bnorm.arcade.rally.internal.racerMove(racer)
}
