package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.rally.Angle
import dev.bnorm.arcade.rally.Car
import kotlin.wasm.unsafe.Pointer

internal class MemoryCar(ptr: Pointer) : Car {
    override val time: Long = (ptr + 0).loadLong()

    override val x: Double = (ptr + 8).loadDouble()
    override val y: Double = (ptr + 16).loadDouble()
    override val heading: Angle = (ptr + 24).loadAngle()
    override val speed: Double = (ptr + 32).loadDouble()

    override val nextCheckpoint: Int = (ptr + 40).loadInt()
}
