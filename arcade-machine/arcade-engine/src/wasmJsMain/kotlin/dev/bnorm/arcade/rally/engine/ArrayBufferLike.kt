package dev.bnorm.arcade.rally.engine

import js.buffer.ArrayBufferLike
import js.numbers.JsNumbers.toJsByte
import js.numbers.JsNumbers.toKotlinByte
import js.typedarrays.Int8Array
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations

fun ArrayBufferLike.toSource(): RawSource {
    return ArrayBufferLikeSource(this)
}

fun ArrayBufferLike.toSink(): RawSink {
    return ArrayBufferLikeSink(this)
}

@OptIn(UnsafeIoApi::class)
internal class ArrayBufferLikeSource(buffer: ArrayBufferLike) : RawSource {
    val upstream = Int8Array(buffer)
    var closed = false
    var offset = 0

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(!closed)
        if (offset >= upstream.length) return -1L
        val bytesRead = minOf(byteCount.toInt(), upstream.length - offset)

        var remaining = bytesRead
        while (remaining > 0) {
            val bytesWritten = UnsafeBufferOperations.writeToTail(
                buffer = sink,
                minimumCapacity = 1,
            ) { bytes, startIndexInclusive, endIndexExclusive ->
                val byteCount = minOf(remaining, endIndexExclusive - startIndexInclusive)
                for (i in 0..<byteCount) {
                    bytes[startIndexInclusive + i] = upstream[offset + i].toKotlinByte()
                }
                byteCount
            }

            remaining -= bytesWritten
            offset += bytesWritten
        }

        return bytesRead.toLong()
    }

    override fun close() {
        closed = true
    }
}

@OptIn(UnsafeIoApi::class)
internal class ArrayBufferLikeSink(buffer: ArrayBufferLike) : RawSink {
    val upstream = Int8Array(buffer)
    var closed = false
    var offset = 0

    override fun write(source: Buffer, byteCount: Long) {
        require(!closed)

        var remaining = byteCount
        while (remaining > 0L) {
            val bytesRead = UnsafeBufferOperations.readFromHead(
                buffer = source,
            ) { bytes, startIndexInclusive, endIndexExclusive ->
                val byteCount = minOf(remaining, (endIndexExclusive - startIndexInclusive).toLong()).toInt()
                for (i in 0..<byteCount) {
                    upstream[offset + 1] = bytes[startIndexInclusive + i].toJsByte()
                }
                byteCount
            }
            remaining -= bytesRead
            offset += bytesRead
        }
    }

    override fun flush() {
        require(!closed)
    }

    override fun close() {
        closed = true
    }
}
