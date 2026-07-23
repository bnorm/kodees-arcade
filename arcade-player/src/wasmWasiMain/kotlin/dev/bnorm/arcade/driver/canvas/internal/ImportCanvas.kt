@file:OptIn(
    ExperimentalWasmInterop::class,
    ComponentModelInternalApi::class,
)

package dev.bnorm.arcade.driver.canvas.internal

import dev.bnorm.arcade.driver.canvas.Canvas
import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.DrawStyle
import dev.bnorm.arcade.driver.canvas.Fill
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
import kotlin.wasm.unsafe.ComponentModelInternalApi
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.freeAllComponentModelReallocAllocatedMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@OptIn(UnsafeWasmMemoryApi::class)
internal object ImportCanvas : Canvas {
    override fun drawSegment(
        color: Color,
        segment: Segment,
        stroke: Stroke,
    ) {
        draw(
            DrawRequest.Segment(
                color = color,
                segment = segment,
                stroke = stroke,
            )
        )
    }

    override fun drawCircle(
        color: Color,
        circle: Circle,
        startAngle: Angle,
        sweepAngle: Angle,
        style: DrawStyle,
    ) {
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

    override fun drawRect(
        color: Color,
        rectangle: Rectangle,
        style: DrawStyle,
    ) {
        draw(
            DrawRequest.Rectangle(
                color = color,
                rectangle = rectangle,
                style = style,
            )
        )
    }

    private fun draw(request: DrawRequest) {
        withScopedMemoryAllocator {
            val variant0: Int
            val variant1: Int
            val variant2: Double
            val variant3: Double
            val variant4: Double
            val variant5: Double
            val variant6: Long
            val variant7: Int
            val variant8: Float

            when (request) {
                is DrawRequest.Segment -> {
                    variant0 = 0
                    variant1 = request.color.value.toInt()
                    variant2 = request.segment.start.x
                    variant3 = request.segment.start.y
                    variant4 = request.segment.end.x
                    variant5 = request.segment.end.y
                    variant6 = request.stroke.width.toRawBits().toLong()
                    variant7 = 0
                    variant8 = 0.0f
                }

                is DrawRequest.Circle -> {
                    variant0 = 1
                    variant1 = request.color.value.toInt()
                    variant2 = request.circle.center.x
                    variant3 = request.circle.center.y
                    variant4 = request.circle.radius
                    variant5 = request.startAngle.radians
                    variant6 = request.sweepAngle.radians.toRawBits()
                    when (val style = request.style) {
                        is Fill -> {
                            variant7 = 0
                            variant8 = 0.0f
                        }

                        is Stroke -> {
                            variant7 = 1
                            variant8 = style.width
                        }
                    }
                }

                is DrawRequest.Rectangle -> {
                    variant0 = 2
                    variant1 = request.color.value.toInt()
                    variant2 = request.rectangle.xRange.start
                    variant3 = request.rectangle.xRange.endInclusive
                    variant4 = request.rectangle.yRange.start
                    variant5 = request.rectangle.yRange.endInclusive
                    when (val style = request.style) {
                        is Fill -> {
                            variant6 = 0L
                            variant7 = 0.0f.toRawBits()
                        }

                        is Stroke -> {
                            variant6 = 1L
                            variant7 = style.width.toRawBits()
                        }
                    }
                    variant8 = 0.0f
                }
            }

            playerCanvasDraw(
                variant0,
                variant1,
                variant2,
                variant3,
                variant4,
                variant5,
                variant6,
                variant7,
                variant8
            )

            freeAllComponentModelReallocAllocatedMemory()
        }
    }
}

@WasmImport(module = "bnorm:arcade/canvas", name = "draw")
private external fun playerCanvasDraw(
    p0: Int,
    p1: Int,
    p2: Double,
    p3: Double,
    p4: Double,
    p5: Double,
    p6: Long,
    p7: Int,
    p8: Float,
)
