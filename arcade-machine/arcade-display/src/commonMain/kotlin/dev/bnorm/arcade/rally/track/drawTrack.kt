package dev.bnorm.arcade.rally.track

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toRelative

// TODO move these to physics?
internal const val TRACK_WIDTH = 90.0
internal const val CAR_WIDTH = 20.0

internal fun DrawScope.drawTrack(
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
