package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.rally.Angle
import kotlin.wasm.unsafe.Pointer

internal fun Pointer.loadUByte(): UByte = loadByte().toUByte()
internal fun Pointer.loadUShort(): UShort = loadShort().toUShort()
internal fun Pointer.loadUInt(): UInt = loadInt().toUInt()
internal fun Pointer.loadULong(): ULong = loadLong().toULong()
internal fun Pointer.loadFloat(): Float = Float.fromBits(loadInt())
internal fun Pointer.loadDouble(): Double = Double.fromBits(loadLong())

internal fun Pointer.loadAngle(): Angle = Angle.ofRadians(loadDouble())

internal fun Pointer.loadByteArray(size: Int): ByteArray = ByteArray(size) { i -> (this + i).loadByte() }
internal fun Pointer.loadString(size: Int): String = loadByteArray(size).decodeToString()
