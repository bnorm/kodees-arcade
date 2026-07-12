@file:OptIn(dev.bnorm.arcade.driver.internal.DriverExport::class, ExperimentalWasmInterop::class)

private val driver = dev.bnorm.arcade.driver.sample.Kodee

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun onRace() {
    dev.bnorm.arcade.driver.internal.driverOnRace(driver)
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun move() {
    dev.bnorm.arcade.driver.internal.driverMove(driver)
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport
fun onDraw() {
    dev.bnorm.arcade.driver.internal.driverOnDraw(driver)
}
