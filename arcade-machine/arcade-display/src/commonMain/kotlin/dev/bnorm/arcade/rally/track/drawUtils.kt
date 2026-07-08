package dev.bnorm.arcade.rally.track

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toRelative

internal const val VISUALLY_STRAIGHT_ENOUGH = 1.0e12

internal fun DrawScope.drawArcsBetween(color: Color, start: Segment, end: Segment) {
    val center = start.toLine().intersect(end.toLine())
    if (center == null) {
        // Parallel segments, so just draw straight lines.
        drawLinesBetween(color, start, end)
    } else {
        val startRadius = center.distanceTo(end.start)
        val endRadius = center.distanceTo(end.end)
        if (maxOf(startRadius, endRadius) > VISUALLY_STRAIGHT_ENOUGH) {
            // Basically parallel segments, so just draw straight lines.
            drawLinesBetween(color, start, end)
        } else {
            val startAngle = center.angleTo(start.start)
            val endAngle = center.angleTo(end.start)
            val sweepAngle = (endAngle - startAngle).toRelative()

            drawArc(
                color = color,
                center = center,
                radius = startRadius,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
            drawArc(
                color = color,
                center = center,
                radius = endRadius,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
        }
    }
}

internal fun DrawScope.drawLinesBetween(color: Color, start: Segment, end: Segment) {
    drawLine(
        color = color,
        start = start.start.toOffset(),
        end = end.start.toOffset(),
        strokeWidth = 4f,
    )
    drawLine(
        color = color,
        start = start.end.toOffset(),
        end = end.end.toOffset(),
        strokeWidth = 4f,
    )
}

internal fun DrawScope.drawArc(
    color: Color,
    center: Point,
    radius: Double,
    startAngle: Angle,
    sweepAngle: Angle,
) {
    drawArc(
        color = color,
        startAngle = -startAngle.degrees.toFloat(),
        sweepAngle = -sweepAngle.degrees.toFloat(),
        useCenter = false,
        topLeft = Offset(
            (center.x - radius).toFloat(),
            (size.height - center.y - radius).toFloat()
        ),
        size = Size(
            (2.0 * radius).toFloat(),
            (2.0 * radius).toFloat(),
        ),
        style = Stroke(width = 4f),
    )
}

internal fun DrawScope.drawSegment(
    color: Color,
    segment: Segment,
    strokeWidth: Float = 4f,
) {
    drawLine(
        color = color,
        start = segment.start.toOffset(),
        end = segment.end.toOffset(),
        strokeWidth = strokeWidth,
    )
}

context(scope: DrawScope)
internal fun Point.toOffset(): Offset {
    return Offset(x.toFloat(), scope.size.height - y.toFloat())
}
