@file:OptIn(dev.bnorm.arcade.driver.internal.DriverExport::class, ExperimentalWasmInterop::class)

private val driver = dev.bnorm.arcade.driver.sample.Snail

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport("bnorm:arcade/driver#on-race")
fun onRace(
    trackWidth: Double,
    trackHeight: Double,
    checkpointsPtr: Int,
    checkpointsCount: Int,
    positionsPtr: Int,
    positionsCount: Int,
    laps: Int
) {
    dev.bnorm.arcade.driver.internal.driverOnRace(
        driver,
        trackWidth,
        trackHeight,
        checkpointsPtr,
        checkpointsCount,
        positionsPtr,
        positionsCount,
        laps,
    )
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport("bnorm:arcade/driver#on-turn")
fun onTurn(
    time: Long,
    x: Double,
    y: Double,
    heading: Double,
    speed: Double,
    lap: Int,
    nextCheckpoint: Int,
) {
    dev.bnorm.arcade.driver.internal.driverMove(
        driver,
        time,
        x,
        y,
        heading,
        speed,
        lap,
        nextCheckpoint,
    )
}

/**
 * Wasm exported function used by the game engine to call our driver.
 */
@WasmExport("bnorm:arcade/driver#on-draw")
fun onDraw() {
    dev.bnorm.arcade.driver.internal.driverOnDraw(
        driver
    )
}
