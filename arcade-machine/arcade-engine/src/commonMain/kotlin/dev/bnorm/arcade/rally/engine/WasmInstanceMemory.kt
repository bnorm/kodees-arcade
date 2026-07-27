package dev.bnorm.arcade.rally.engine

import kotlinx.io.Sink

interface WasmInstanceMemory {
    fun require(byteCount: Int)

    fun readBytesTo(sink: Sink, offset: Int, length: Int)
    fun readInt32(offset: Int): Int

    fun writeBytes(offset: Int, src: ByteArray)
    fun writeInt32(offset: Int, value: Int)
    fun writeFloat64(offset: Int, value: Double)
}
