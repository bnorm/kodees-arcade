package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.Fill
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
import kotlin.random.Random
import kotlinx.io.Sink

internal fun fdWrite(
    memory: WasmInstanceMemory,
    iovs: Int,
    iovs_len: Int,
    fd: Int,
    nwritten: Int,
    stdout: Sink,
    stderr: Sink,
): Int {
    var bytesWritten = 0

    val sink = when (fd) {
        1 -> stdout
        2 -> stderr
        else -> {
            memory.writeInt32(nwritten, 0)
            return 1
        }
    }

    for (i in 0..<iovs_len) {
        val offset = memory.readInt32(iovs + i * 8)
        val length = memory.readInt32(iovs + i * 8 + 4)
        memory.readBytesTo(sink, offset, length)
        bytesWritten += length
    }

    memory.writeInt32(nwritten, bytesWritten)
    return 0
}

internal fun randomGet(
    memory: WasmInstanceMemory,
    random: Random,
    bufLen: Int,
    bufPtr: Int
): Int {
    val randomBytes = random.nextBytes(bufLen)
    memory.writeBytes(bufPtr, randomBytes)
    return 0
}

internal fun readDrawRequest(
    p0: Int,
    p1: Int,
    p2: Double,
    p3: Double,
    p4: Double,
    p5: Double,
    p6: Long,
    p7: Int,
    p8: Float,
): DrawRequest {
    return when (p0) {
        0 -> DrawRequest.Segment(
            color = Color(p1.toUInt()),
            segment = Segment(
                start = Point(
                    x = p2,
                    y = p3,
                ),
                end = Point(
                    x = p4,
                    y = p5,
                )
            ),
            stroke = Stroke(Float.fromBits(p6.toInt())),
        )

        1 -> DrawRequest.Circle(
            color = Color(p1.toUInt()),
            circle = Circle(
                center = Point(
                    x = p2,
                    y = p3,
                ),
                radius = p4,
            ),
            startAngle = Angle.ofRadians(p5),
            sweepAngle = Angle.ofRadians(Double.fromBits(p6)),
            style = when (p7) {
                0 -> Fill
                1 -> Stroke(
                    width = p8,
                )

                else -> error("!")
            }
        )

        2 -> DrawRequest.Rectangle(
            color = Color(p1.toUInt()),
            rectangle = run {
                Rectangle(
                    minX = p2,
                    maxX = p3,
                    minY = p4,
                    maxY = p5,
                )
            },
            style = when (p6) {
                0L -> Fill
                1L -> Stroke(
                    width = Float.fromBits(p7),
                )

                else -> error("!")
            }
        )

        else -> error("!")
    }
}
