@file:OptIn(ExperimentalSerializationApi::class)

package dev.bnorm.arcade.driver.internal

import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf

@UnsafeWasmMemoryApi
internal fun Pointer.loadByteArray(size: Int): ByteArray = ByteArray(size) { i -> (this + i).loadByte() }

@UnsafeWasmMemoryApi
internal fun <T> Pointer.loadProtoBuf(serializer: KSerializer<T>): T {
    val size = loadInt()
    val bytes = (this + 4).loadByteArray(size)
    return ProtoBuf.decodeFromByteArray(serializer, bytes)
}

@UnsafeWasmMemoryApi
internal fun Pointer.storeByteArray(data: ByteArray) {
    var currentPtr = this
    for (datum in data) {
        currentPtr.storeByte(datum)
        currentPtr += 1
    }
}

@UnsafeWasmMemoryApi
internal fun <T> Pointer.storeProtoBuf(serializer: SerializationStrategy<T>, value: T) {
    val data = ProtoBuf.encodeToByteArray(serializer, value)
    storeInt(data.size)
    (this + 4).storeByteArray(data)
}

@UnsafeWasmMemoryApi
internal fun <T> MemoryAllocator.allocateProtoBuf(serializer: SerializationStrategy<T>, value: T): Pointer {
    val data = ProtoBuf.encodeToByteArray(serializer, value)
    val ptr = allocate(4 + data.size)
    ptr.storeInt(data.size)
    (ptr + 4).storeByteArray(data)
    return ptr
}
