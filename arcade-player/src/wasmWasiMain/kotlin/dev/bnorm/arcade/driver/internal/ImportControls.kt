@file:OptIn(
    ExperimentalWasmInterop::class,
    UnsafeWasmMemoryApi::class,
    ComponentModelInternalApi::class,
)

package dev.bnorm.arcade.driver.internal

import dev.bnorm.arcade.driver.Controls
import kotlin.wasm.unsafe.ComponentModelInternalApi
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.freeAllComponentModelReallocAllocatedMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal object ImportControls : Controls {
    override var throttle: Double
        get() {
            withScopedMemoryAllocator {
                val throttle = getControlsThrottle()
                freeAllComponentModelReallocAllocatedMemory()
                return throttle
            }
        }
        set(value) {
            withScopedMemoryAllocator {
                setControlsThrottle(value)
                freeAllComponentModelReallocAllocatedMemory()
            }
        }

    override var steering: Double
        get() {
            withScopedMemoryAllocator {
                val steering = getControlsSteering()
                freeAllComponentModelReallocAllocatedMemory()
                return steering
            }
        }
        set(value) {
            withScopedMemoryAllocator {
                setControlsSteering(value)
                freeAllComponentModelReallocAllocatedMemory()
            }
        }
}

@WasmImport(module = "bnorm:arcade/controls", name = "throttle-get")
private external fun getControlsThrottle(): Double

@WasmImport(module = "bnorm:arcade/controls", name = "throttle-set")
private external fun setControlsThrottle(throttle: Double)

@WasmImport(module = "bnorm:arcade/controls", name = "steering-get")
private external fun getControlsSteering(): Double

@WasmImport(module = "bnorm:arcade/controls", name = "steering-set")
private external fun setControlsSteering(steering: Double)
