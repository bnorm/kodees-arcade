package dev.bnorm.arcade.rally.engine.wasm

import js.buffer.DataView
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import web.assembly.Memory

@OptIn(ExperimentalSerializationApi::class)
class BrowserMemory(
    internal val delegate: Memory<*>,
) : WasmMemory {
    override fun <T> writeProto(offset: Int, serializer: KSerializer<T>, value: T) {
        val bytes = ProtoBuf.encodeToByteArray(serializer, value)

        val pages = offset + 4 + bytes.size
        if (delegate.buffer.byteLength < pages) {
            delegate.grow(pages - delegate.buffer.byteLength)
        }

        val view = DataView(delegate.buffer, byteOffset = offset)
        view.setInt32(0, bytes.size, littleEndian = true)
        repeat(bytes.size) {
            view.setInt8(4 + it, bytes[it])
        }
    }
}
