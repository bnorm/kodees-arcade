package dev.bnorm.arcade.display.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.bnorm.arcade.display.track.size
import dev.bnorm.arcade.display.track.toOffset
import dev.bnorm.arcade.display.track.topLeft
import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.DrawStyle
import dev.bnorm.arcade.driver.canvas.Fill
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.geometry.Angle
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.DrawStyle as ComposeDrawStyle
import androidx.compose.ui.graphics.drawscope.Fill as ComposeFill
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke

context(scope: DrawScope)
internal fun DrawRequest.draw() {
    when (this) {
        is DrawRequest.Segment -> scope.drawLine(
            color = color.toComposeColor(),
            start = segment.start.toOffset(),
            end = segment.end.toOffset(),
            strokeWidth = stroke.width,
        )

        is DrawRequest.Circle -> when (isArc()) {
            true -> {
                val radius = circle.radius.toFloat()
                val size = 2f * radius
                scope.drawArc(
                    color = color.toComposeColor(),
                    startAngle = -startAngle.degrees.toFloat(), // Compose rotates clockwise.
                    sweepAngle = -sweepAngle.degrees.toFloat(), // Compose rotates clockwise.
                    topLeft = circle.center.toOffset() - Offset(radius, radius),
                    size = Size(size, size),
                    useCenter = false,
                    style = style.toComposeDrawStyle(),
                )
            }

            false -> {
                scope.drawCircle(
                    color = color.toComposeColor(),
                    radius = circle.radius.toFloat(),
                    center = circle.center.toOffset(),
                    style = style.toComposeDrawStyle(),
                )
            }
        }

        is DrawRequest.Rectangle -> {
            scope.drawRect(
                color = color.toComposeColor(),
                topLeft = rectangle.topLeft,
                size = rectangle.size,
                style = style.toComposeDrawStyle(),
            )
        }
    }
}

private fun DrawRequest.Circle.isArc(): Boolean = sweepAngle > -Angle.FULL_CIRCLE && sweepAngle < Angle.FULL_CIRCLE

private fun Color.toComposeColor(): ComposeColor = ComposeColor(value.toInt())

private fun DrawStyle.toComposeDrawStyle(): ComposeDrawStyle = when (this) {
    Fill -> ComposeFill
    is Stroke -> ComposeStroke(width)
}
