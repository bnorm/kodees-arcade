package dev.bnorm.arcade

class MemoryCache<T : Any> : Cache<T> {
    private val map = mutableMapOf<String, T>()

    override val keys: Set<String>
        get() = map.keys

    override fun get(key: String): T? {
        return map[key]
    }

    override fun set(key: String, value: T) {
        map[key] = value
    }

    override fun remove(key: String): T? {
        return map.remove(key)
    }
}

