package dev.bnorm.arcade.rally.engine

import ai.tegmentum.wasmtime4j.WasmMemory
import kotlinx.io.DelicateIoApi
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.writeToInternalBuffer

class WasmtimeWasmInstanceMemory(
    private val memory: WasmMemory
) : WasmInstanceMemory {
    override fun require(byteCount: Int) {
        val pageCount = memory.size
        val pageSize = memory.pageSize()
        val pages = byteCount / pageSize + (byteCount % pageSize).coerceAtMost(1)
        if (pageCount < pages) {
            if (memory.grow(pages - pageCount) == -1) {
                error("unable to allocate sufficient memory: $byteCount")
            }
        }
    }

    @OptIn(UnsafeIoApi::class, DelicateIoApi::class)
    override fun readBytesTo(sink: Sink, offset: Int, length: Int) {
        var remaining = length
        var offset = offset
        sink.writeToInternalBuffer { buffer ->
            while (remaining > 0) {
                val length = minOf(remaining, 8192)
                val bytesWritten = UnsafeBufferOperations.writeToTail(
                    buffer = buffer,
                    minimumCapacity = length,
                ) { dest, destOffset, _ ->
                    memory.readBytes(offset, dest, destOffset, length)
                    length
                }

                remaining -= bytesWritten
                offset += bytesWritten
            }
        }
    }

    override fun readInt32(offset: Int): Int {
        return memory.readInt32(offset.toLong())
    }

    override fun writeBytes(offset: Int, src: ByteArray) {
        memory.writeBytes(offset, src, 0, src.size)
    }

    override fun writeInt32(offset: Int, value: Int) {
        memory.writeInt32(offset.toLong(), value)
    }

    override fun writeFloat64(offset: Int, value: Double) {
        memory.writeFloat64(offset.toLong(), value)
    }
}
