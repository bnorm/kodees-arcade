package dev.bnorm.arcade.rally.engine

import js.buffer.DataView
import js.numbers.JsNumbers.toJsByte
import js.numbers.JsNumbers.toKotlinByte
import js.typedarrays.Int8Array
import kotlinx.io.DelicateIoApi
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.writeToInternalBuffer
import web.assembly.Memory

class BrowserWasmInstanceMemory(
    private val memory: Memory<*>
) : WasmInstanceMemory {
    private var view = DataView(memory.buffer)

    override fun require(byteCount: Int) {
        val byteLength = memory.buffer.byteLength
        if (byteLength < byteCount) {
            if (memory.grow(byteCount - byteLength) == -1) {
                error("unable to allocate sufficient memory: $byteCount")
            }
        }
        view = DataView(memory.buffer)
    }

    @OptIn(UnsafeIoApi::class, DelicateIoApi::class)
    override fun readBytesTo(sink: Sink, offset: Int, length: Int) {
        val source = Int8Array(memory.buffer)

        var remaining = length
        var offset = offset
        sink.writeToInternalBuffer { buffer ->
            while (remaining > 0) {
                val bytesWritten = UnsafeBufferOperations.writeToTail(
                    buffer = buffer,
                    minimumCapacity = 1,
                ) { bytes, startIndexInclusive, endIndexExclusive ->
                    val byteCount = minOf(remaining, endIndexExclusive - startIndexInclusive)
                    for (i in 0..<byteCount) {
                        bytes[startIndexInclusive + i] = source[offset + i].toKotlinByte()
                    }
                    byteCount
                }

                remaining -= bytesWritten
                offset += bytesWritten
            }
        }
    }

    override fun readInt32(offset: Int): Int {
        return view.getInt32(offset, littleEndian = true)
    }

    override fun writeBytes(offset: Int, src: ByteArray) {
        val sink = Int8Array(memory.buffer.slice(offset))
        for (i in src.indices) {
            sink[i] = src[i].toJsByte()
        }
    }

    override fun writeInt32(offset: Int, value: Int) {
        view.setInt32(offset, value, littleEndian = true)
    }

    override fun writeFloat64(offset: Int, value: Double) {
        view.setFloat64(offset, value, littleEndian = true)
    }
}
