package dev.bnorm.arcade

interface Cache<T : Any> {
    val keys: Set<String>
    operator fun get(key: String): T?
    operator fun set(key: String, value: T)
    fun remove(key: String): T?
}
