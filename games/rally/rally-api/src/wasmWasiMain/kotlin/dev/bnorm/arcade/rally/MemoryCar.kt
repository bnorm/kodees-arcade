@file:OptIn(UnsafeWasmMemoryApi::class)

package dev.bnorm.arcade.rally

import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

internal class MemoryCar(ptr: Pointer) : Car {
    override val time: Long = (ptr + 0 * 8).loadLong()

    override val x: Double = (ptr + 1 * 8).loadDouble()
    override val y: Double = (ptr + 2 * 8).loadDouble()
    override val heading: Angle = (ptr + 3 * 8).loadAngle()
    override val speed: Double = (ptr + 4 * 8).loadDouble()

    override fun toString(): String {
        return "MemoryCar(time=$time, x=$x, y=$y, heading=$heading, speed=$speed)"
    }
}

private fun Pointer.loadUByte(): UByte = loadByte().toUByte()
private fun Pointer.loadUShort(): UShort = loadShort().toUShort()
private fun Pointer.loadUInt(): UInt = loadInt().toUInt()
private fun Pointer.loadULong(): ULong = loadLong().toULong()
private fun Pointer.loadFloat(): Float = Float.fromBits(loadInt())
private fun Pointer.loadDouble(): Double = Double.fromBits(loadLong())

private fun Pointer.loadAngle(): Angle = Angle.ofRadians(loadDouble())

private fun Pointer.loadByteArray(size: Int): ByteArray = ByteArray(size) { i -> (this + i).loadByte() }
private fun Pointer.loadString(size: Int): String = loadByteArray(size).decodeToString()
