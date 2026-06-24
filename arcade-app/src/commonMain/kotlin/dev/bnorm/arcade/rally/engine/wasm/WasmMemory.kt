package dev.bnorm.arcade.rally.engine.wasm

import kotlinx.serialization.KSerializer

interface WasmMemory {
    fun <T> writeProto(offset: Int, serializer: KSerializer<T>, value: T)
}
