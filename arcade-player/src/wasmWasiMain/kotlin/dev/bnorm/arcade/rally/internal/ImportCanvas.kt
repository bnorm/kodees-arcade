@file:OptIn(ExperimentalWasmInterop::class)

package dev.bnorm.arcade.rally.internal

import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.rally.Canvas
import dev.bnorm.arcade.rally.Color
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@OptIn(UnsafeWasmMemoryApi::class)
internal object ImportCanvas : Canvas {
    override fun drawSegment(color: Color, segment: Segment) {
        withScopedMemoryAllocator { allocator ->
            val ptr = allocator.allocateProtoBuf(Segment.serializer(), segment)
            drawSegment(color.value, ptr.address)
        }
    }

    override fun drawCircle(color: Color, circle: Circle) {
        withScopedMemoryAllocator { allocator ->
            val ptr = allocator.allocateProtoBuf(Circle.serializer(), circle)
            drawCircle(color.value, ptr.address)
        }
    }

    override fun fillCircle(color: Color, circle: Circle) {
        withScopedMemoryAllocator { allocator ->
            val ptr = allocator.allocateProtoBuf(Circle.serializer(), circle)
            fillCircle(color.value, ptr.address)
        }
    }

    override fun fillRect(color: Color, rectangle: Rectangle) {
        withScopedMemoryAllocator { allocator ->
            val ptr = allocator.allocateProtoBuf(Rectangle.serializer(), rectangle)
            fillRect(color.value, ptr.address)
        }
    }
}

@WasmImport(module = "player_canvas", name = "draw_segment")
private external fun drawSegment(color: UInt, segmentPtr: UInt)

@WasmImport(module = "player_canvas", name = "draw_circle")
private external fun drawCircle(color: UInt, circlePtr: UInt)

@WasmImport(module = "player_canvas", name = "fill_circle")
private external fun fillCircle(color: UInt, circlePtr: UInt)

@WasmImport(module = "player_canvas", name = "fill_rect")
private external fun fillRect(color: UInt, rectPtr: UInt)
