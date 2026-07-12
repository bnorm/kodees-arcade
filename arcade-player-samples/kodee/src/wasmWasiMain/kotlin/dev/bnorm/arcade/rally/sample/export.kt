@file:OptIn(dev.bnorm.arcade.rally.internal.DriverExport::class, ExperimentalWasmInterop::class)

private val driver = dev.bnorm.arcade.rally.sample.Kodee

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun onRace() {
    dev.bnorm.arcade.rally.internal.driverOnRace(driver)
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun move() {
    dev.bnorm.arcade.rally.internal.driverMove(driver)
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun onDraw() {
    dev.bnorm.arcade.rally.internal.driverOnDraw(driver)
}
