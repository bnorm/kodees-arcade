@file:OptIn(ExperimentalWasmInterop::class)

package dev.bnorm.arcade.rally

internal object WasmImportControls : Controls {
    override var throttle: Double
        get() = getControlsThrottle()
        set(value) = setControlsThrottle(value)

    override var steering: Double
        get() = getControlsSteering()
        set(value) = setControlsSteering(value)
}

@WasmImport(module = "rally_api", name = "controls_throttle_get")
private external fun getControlsThrottle(): Double

@WasmImport(module = "rally_api", name = "controls_throttle_set")
private external fun setControlsThrottle(throttle: Double)

@WasmImport(module = "rally_api", name = "controls_steering_get")
private external fun getControlsSteering(): Double

@WasmImport(module = "rally_api", name = "controls_steering_set")
private external fun setControlsSteering(steering: Double)
