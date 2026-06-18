package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.rally.Point
import dev.bnorm.arcade.rally.Track
import kotlin.wasm.unsafe.Pointer

internal class MemoryTrack(ptr: Pointer) : Track {
    private val checkpointSize = (ptr + 0).loadInt()
    override val checkpoints: List<Track.Checkpoint> = buildList {
        var ptr = (ptr + 4)
        repeat(checkpointSize) {
            add(MemoryCheckpoint(ptr))
            ptr += MemoryCheckpoint.msize
        }
    }

    private class MemoryCheckpoint(ptr: Pointer) : Track.Checkpoint {
        companion object {
            const val msize = 32
        }

        override val start: Point = MemoryPoint(ptr)
        override val end: Point = MemoryPoint(ptr + MemoryPoint.msize)
    }

    private class MemoryPoint(ptr: Pointer) : Point {
        companion object {
            const val msize = 16
        }

        override val x: Double = (ptr + 0).loadDouble()
        override val y: Double = (ptr + 8).loadDouble()
    }
}
