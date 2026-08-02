package dev.bnorm.arcade.display.game

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot

private const val MAX_STDOUT_LENGTH = 100_000

@Stable
class TextLines(
    // 0 indicates infinite.
    private val lengthLimit: Int = MAX_STDOUT_LENGTH,
) {
    init {
        require(lengthLimit >= 0) { "Length limit must be positive" }
    }

    private var firstLine by mutableLongStateOf(0L)
    private val textLines = mutableStateListOf<String>()
    private var last by mutableStateOf("")
    private val columnCounts = mutableStateMapOf<Int, Int>()

    var length by mutableStateOf(0)
        private set

    var columns by mutableStateOf(0)
        private set

    val lines: LongRange
        get() = firstLine..(firstLine + textLines.size)

    fun getLine(index: Long): String {
        return if (index == firstLine + textLines.size) last else textLines[(index - firstLine).toInt()]
    }

    fun append(text: String) {
        Snapshot.withMutableSnapshot {
            val start = textLines.size

            // Add new text lines to the list.
            val split = text.split('\n')
            if (split.size > 1) {
                textLines.add(if (last.isEmpty()) split[0] else last + split[0])
                textLines.addAll(split.subList(1, split.lastIndex))
            }
            last = if (last.isEmpty()) split.last() else last + split.last()

            // Add new text to the length.
            length += text.length

            // Compare new text lines against known max columns.
            for (i in start..<textLines.size) {
                val length = textLines[i].length
                columnCounts[length] = columnCounts.getOrElse(length) { 0 } + 1
                columns = maxOf(columns, length)
            }
            columns = maxOf(columns, last.length)

            // Truncate
            // TODO instead of iteratively...
            //  - find all lines that need to be removed first
            //  - grab sublist and remove from column counts
            //    - calculate new max columns once, if any match
            //  - clear sublist
            //  this should hopefully make the removal from text lines more efficient
            //  - will multi-line clears happen enough to make this worth it?
            // TODO should we even make the columns value smaller?
            //  - is the UX actually worth the effort?
            while (lengthLimit in 1..<length) {
                firstLine++
                val line = textLines.removeAt(0)
                val length = line.length
                this.length -= length
                val newCount = columnCounts.getValue(length) - 1
                if (newCount > 0) {
                    columnCounts[length] = newCount
                } else {
                    columnCounts.remove(length)
                    if (columns == length) {
                        columns = columnCounts.keys.maxOrNull() ?: last.length
                        columns = maxOf(columns, last.length)
                    }
                }
            }
        }
    }
}
