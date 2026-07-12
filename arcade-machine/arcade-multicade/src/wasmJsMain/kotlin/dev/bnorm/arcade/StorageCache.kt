package dev.bnorm.arcade

import org.w3c.dom.Storage

class StorageCache(
    private val storage: Storage,
) : Cache<String> {
    override val keys: Set<String>
        field = KeySet()

    override fun get(key: String): String? {
        return storage.getItem(key)
    }

    override fun set(key: String, value: String) {
        storage.setItem(key, value)
    }

    override fun remove(key: String): String? {
        return storage.getItem(key).also {
            storage.removeItem(key)
        }
    }

    private inner class KeySet : Set<String> {
        override val size: Int
            get() = storage.length

        override fun isEmpty(): Boolean {
            return size == 0
        }

        override fun contains(element: String): Boolean {
            return storage.getItem(element) != null
        }

        override fun iterator(): Iterator<String> {
            return iterator {
                for (i in 0..<storage.length) {
                    val key = storage.key(i) ?: break
                    yield(key)
                }
            }
        }

        override fun containsAll(elements: Collection<String>): Boolean {
            return elements.all { contains(it) }
        }
    }
}
