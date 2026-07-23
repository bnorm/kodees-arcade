package dev.bnorm.arcade.driver.canvas.internal

import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.DrawStyle
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle as CircleShape
import dev.bnorm.arcade.geometry.Rectangle as RectangleShape
import dev.bnorm.arcade.geometry.Segment as SegmentShape

sealed class DrawRequest {
    class Segment(
        val color: Color,
        val segment: SegmentShape,
        val stroke: Stroke,
    ) : DrawRequest()

    class Circle(
        val color: Color,
        val circle: CircleShape,
        val startAngle: Angle,
        val sweepAngle: Angle,
        val style: DrawStyle
    ) : DrawRequest()

    class Rectangle(
        val color: Color,
        val rectangle: RectangleShape,
        val style: DrawStyle,
    ) : DrawRequest()
}
