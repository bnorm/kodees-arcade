package dev.bnorm.arcade.rally.track

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toPoint
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.geometry.toVector

// TODO move these to physics?
internal const val TRACK_WIDTH = 90.0
internal const val CAR_WIDTH = 20.0

private val STARTING_GRID = Path().apply {
    val size = CAR_WIDTH.toFloat()
    moveTo(size / 4, size / 2)
    relativeLineTo(size / 4, 0f)
    relativeLineTo(0f, -size)
    relativeLineTo(-size / 4, 0f)
}

internal fun DrawScope.drawTrack(
    checkpoints: List<Segment>,
    startingLine: Segment?,
    positions: List<Position>,
    complete: Boolean,
) {
    drawPavement(checkpoints, complete)
    if (startingLine != null) drawStartingLine(startingLine)
    drawStartingGrid(positions)
}

private fun DrawScope.drawPavement(
    checkpoints: List<Segment>,
    complete: Boolean
) {
    // TODO There seems to be a weird bug in Compose/Skia when drawing thick arcs.
    //  - At just the right radius, there's a little artifact that appears
    //    before and/or after the start and/or end sweep of the arc.
    //  - Maybe it's browser specific? Maybe it's Compose Multiplatform specific?
    //    Maybe it's a problem with Skia?
    //  - Maybe it's a Path problem, and we should go back to drawing everything manually?
    val centerLine = checkpoints.toCenterLine(this.size, complete = complete)
    val dashLength = 25f
    drawPath(centerLine, color = Color.Black, style = Stroke(width = TRACK_WIDTH.toFloat() + 6f))
    drawPath(
        centerLine,
        color = Color.White,
        style = Stroke(
            width = TRACK_WIDTH.toFloat() + 5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength), phase = dashLength)
        )
    )
    drawPath(centerLine, color = Color.Black, style = Stroke(width = TRACK_WIDTH.toFloat() + 1f))
    drawPath(centerLine, color = Color.DarkGray, style = Stroke(width = TRACK_WIDTH.toFloat()))
}

private fun DrawScope.drawStartingLine(segment: Segment) {
    val vector = (segment.end - segment.start).toVector()
    val repeat = (vector.magnitude / 8.0).toInt()

    val move = Vector(vector.angle, magnitude = vector.magnitude / repeat).toPoint()
    val offset = Vector(vector.angle + Angle.QUARTER_CIRCLE, magnitude = 4.0).toPoint()

    var flip = true
    repeat(repeat) {
        val start = segment.start + move * it.toDouble()
        val stop = segment.start + move * (it + 1).toDouble()
        drawLine(
            if (flip) Color.Black else Color.White,
            start = (start + offset).toOffset(),
            end = (stop + offset).toOffset(),
            strokeWidth = 8f,
        )
        drawLine(
            if (flip) Color.White else Color.Black,
            start = (start - offset).toOffset(),
            end = (stop - offset).toOffset(),
            strokeWidth = 8f,
        )
        flip = !flip
    }
}

private fun DrawScope.drawStartingGrid(positions: List<Position>) {
    for (position in positions) {
        val location = position.location.toOffset()
        rotate(-position.heading.degrees.toFloat(), location) {
            translate(location.x, location.y) {
                drawPath(STARTING_GRID, color = Color.LightGray, style = Stroke(width = 2f))
            }
        }
    }
}

fun List<Segment>.toCenterLine(size: Size, complete: Boolean = true): Path {
    val centerLine = Path()
    if (this.isEmpty()) return centerLine

    fun pathToNext(prev: Segment, next: Segment) {
        val intersect = prev.toLine().intersect(next.toLine())
        val nextCenter = next.center

        if (intersect == null) {
            // Parallel segments, so just draw straight lines.
            centerLine.lineTo(nextCenter.x.toFloat(), size.height - nextCenter.y.toFloat())
        } else {
            val prevCenter = prev.center
            val radius = intersect.distanceTo(prevCenter)
            if (radius > VISUALLY_STRAIGHT_ENOUGH) {
                // Basically parallel segments, so just draw straight lines.
                centerLine.lineTo(nextCenter.x.toFloat(), size.height - nextCenter.y.toFloat())
            } else {
                val startAngle = intersect.angleTo(prevCenter)
                val endAngle = intersect.angleTo(nextCenter)
                val sweepAngle = (endAngle - startAngle).toRelative()

                centerLine.arcTo(
                    rect = Rect(
                        offset = Offset(
                            (intersect.x - radius).toFloat(),
                            (size.height - intersect.y - radius).toFloat()
                        ),
                        size = Size(
                            (2.0 * radius).toFloat(),
                            (2.0 * radius).toFloat(),
                        ),
                    ),
                    startAngleDegrees = -startAngle.degrees.toFloat(),
                    sweepAngleDegrees = -sweepAngle.degrees.toFloat(),
                    forceMoveTo = false,
                )
            }
        }
    }

    var previous: Segment? = null
    for (current in this) {
        if (previous != null) {
            pathToNext(previous, current)
        } else {
            val center = current.center
            centerLine.moveTo(center.x.toFloat(), size.height - center.y.toFloat())
        }
        previous = current
    }

    if (complete && previous != null) {
        pathToNext(previous, this.first())
    }

    return centerLine
}
