package dev.bnorm.arcade

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileSystemCache(
    private val directory: Path,
) : Cache<String> {
    override val keys: Set<String>
        field = KeySet()

    override fun get(key: String): String? {
        return directory.resolve(key)
            .takeIf { it.exists() }
            ?.readText()
    }

    override fun set(key: String, value: String) {
        val path = directory.resolve(key)
        path.writeText(value)
    }

    override fun remove(key: String): String? {
        val path = directory.resolve(key)
            .takeIf { it.exists() }
        val value = path?.readText()
        path?.deleteExisting()
        return value
    }

    private inner class KeySet : Set<String> {
        override val size: Int
            get() = iterator().asSequence().count()

        override fun isEmpty(): Boolean {
            return size == 0
        }

        override fun contains(element: String): Boolean {
            return directory.resolve(element).exists()
        }

        override fun iterator(): Iterator<String> {
            return iterator {
                directory.forEachDirectoryEntry {
                    yield(it.name)
                }
            }
        }

        override fun containsAll(elements: Collection<String>): Boolean {
            // TODO iterator().toSet().containsAll(elements)?
            return elements.all { contains(it) }
        }
    }
}
