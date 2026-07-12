@file:OptIn(ExperimentalWasmInterop::class)

package dev.bnorm.arcade.driver.canvas.internal

import dev.bnorm.arcade.driver.canvas.Canvas
import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.DrawStyle
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.driver.internal.allocateProtoBuf
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@OptIn(UnsafeWasmMemoryApi::class)
internal object ImportCanvas : Canvas {
    override fun drawSegment(color: Color, segment: Segment, stroke: Stroke) {
        draw(
            DrawRequest.Segment(
                color = color,
                segment = segment,
                stroke = stroke,
            )
        )
    }

    override fun drawCircle(color: Color, circle: Circle, startAngle: Angle, sweepAngle: Angle, style: DrawStyle) {
        draw(
            DrawRequest.Circle(
                color = color,
                circle = circle,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                style = style,
            )
        )
    }

    override fun drawRect(color: Color, rectangle: Rectangle, style: DrawStyle) {
        draw(
            DrawRequest.Rectangle(
                color = color,
                rectangle = rectangle,
                style = style,
            )
        )
    }

    private fun draw(request: DrawRequest) {
        withScopedMemoryAllocator { allocator ->
            val ptr = allocator.allocateProtoBuf(DrawRequest.serializer(), request)
            playerCanvasDraw(ptr.address)
        }
    }
}

@WasmImport(module = "player_canvas", name = "draw")
private external fun playerCanvasDraw(offset: UInt)
