package dev.bnorm.arcade

class NestedCache<T : Any>(
    delegate: Cache<T>,
    part: String,
) : Cache<T> {
    private val delegate: Cache<T>
    private val part: String

    init {
        // Unwrap nested of nested caches.
        if (delegate is NestedCache<T>) {
            this.delegate = delegate.delegate
            this.part = "${delegate.part}.$part"
        } else {
            this.delegate = delegate
            this.part = part
        }
    }

    override val keys: Set<String>
        field = KeySet()

    override fun get(key: String): T? {
        return delegate["$part.$key"]
    }

    override fun set(key: String, value: T) {
        delegate["$part.$key"] = value
    }

    override fun remove(key: String): T? {
        return delegate["$part.$key"]
    }

    private inner class KeySet : Set<String> {
        private val delegateKeys = delegate.keys

        override val size: Int
            get() = delegateKeys.count { it.startsWith(".") }

        override fun isEmpty(): Boolean {
            return delegateKeys.count { it.startsWith(".") } == 0
        }

        override fun contains(element: String): Boolean {
            return delegateKeys.contains("$part.$element")
        }

        override fun iterator(): Iterator<String> {
            return iterator {
                for (key in delegateKeys) {
                    key.removePrefix("$part.")
                        .takeIf { it != key }
                        ?.let { yield(it) }
                }
            }
        }

        override fun containsAll(elements: Collection<String>): Boolean {
            return delegateKeys.containsAll(elements.map { "$part.$it" })
        }
    }
}
