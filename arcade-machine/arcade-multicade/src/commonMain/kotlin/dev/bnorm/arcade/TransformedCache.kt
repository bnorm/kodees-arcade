package dev.bnorm.arcade

class TransformedCache<R : Any, T : Any>(
    private val delegate: Cache<T>,
    private val getter: (T) -> R,
    private val setter: (R) -> T,
) : Cache<R> {
    override val keys: Set<String>
        get() = delegate.keys

    override operator fun get(key: String): R? {
        val value = delegate[key] ?: return null
        return getter(value)
    }

    override operator fun set(key: String, value: R) {
        delegate[key] = setter(value)
    }

    override fun remove(key: String): R? {
        val value = delegate.remove(key) ?: return null
        return getter(value)
    }
}
