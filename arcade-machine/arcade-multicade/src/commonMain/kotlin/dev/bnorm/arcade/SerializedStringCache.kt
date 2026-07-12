package dev.bnorm.arcade

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat

class SerializedStringCache<T : Any>(
    private val delegate: Cache<String>,
    private val serializer: KSerializer<T>,
    private val format: StringFormat,
) : Cache<T> {
    override val keys: Set<String>
        get() = delegate.keys

    override operator fun get(key: String): T? {
        val value = delegate[key] ?: return null
        return format.decodeFromString(serializer, value)
    }

    override operator fun set(key: String, value: T) {
        delegate[key] = format.encodeToString(serializer, value)
    }

    override fun remove(key: String): T? {
        val value = delegate.remove(key) ?: return null
        return format.decodeFromString(serializer, value)
    }
}
