package dev.bnorm.arcade.rally.engine.wasm

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class Wasmtime4jMemory(
    private val delegate: ai.tegmentum.wasmtime4j.WasmMemory,
) : WasmMemory {
    override fun <T> writeProto(offset: Int, serializer: KSerializer<T>, value: T) {
        val bytes = ProtoBuf.encodeToByteArray(serializer, value)

        val byteCount = offset + 4 + bytes.size
        val pages = byteCount / delegate.pageSize() + (byteCount % delegate.pageSize()).coerceAtMost(1)
        if (delegate.size < pages) {
            delegate.grow(pages - delegate.size)
        }

        delegate.writeInt32(offset.toLong(), bytes.size)
        delegate.writeBytes(offset + 4, bytes, 0, bytes.size)
    }
}
