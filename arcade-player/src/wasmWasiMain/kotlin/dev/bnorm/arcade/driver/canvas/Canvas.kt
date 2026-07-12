package dev.bnorm.arcade.driver.canvas

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment

interface Canvas {
    fun drawSegment(color: Color, segment: Segment, stroke: Stroke)

    fun drawCircle(color: Color, circle: Circle, style: DrawStyle = Fill) {
        drawCircle(color, circle, startAngle = Angle.ZERO, sweepAngle = Angle.FULL_CIRCLE, style)
    }

    fun drawCircle(color: Color, circle: Circle, startAngle: Angle, sweepAngle: Angle, style: DrawStyle = Fill)

    fun drawRect(color: Color, rectangle: Rectangle, style: DrawStyle = Fill)
}
