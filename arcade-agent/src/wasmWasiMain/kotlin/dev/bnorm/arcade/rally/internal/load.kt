@file:OptIn(ExperimentalSerializationApi::class)

package dev.bnorm.arcade.rally.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@UnsafeWasmMemoryApi
internal fun Pointer.loadByteArray(size: Int): ByteArray = ByteArray(size) { i -> (this + i).loadByte() }

@UnsafeWasmMemoryApi
internal fun <T> Pointer.loadProtoBuf(serializer: KSerializer<T>): T {
    val size = loadInt()
    val bytes = (this + 4).loadByteArray(size)
    return ProtoBuf.decodeFromByteArray(serializer, bytes)
}
