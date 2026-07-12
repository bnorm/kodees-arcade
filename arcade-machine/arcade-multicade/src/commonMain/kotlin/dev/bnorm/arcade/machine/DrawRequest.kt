package dev.bnorm.arcade.machine

import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment

sealed class DrawRequest {
    class DrawSegment(val color: UInt, val segment: Segment) : DrawRequest()
    class DrawCircle(val color: UInt, val circle: Circle) : DrawRequest()
    class FillCircle(val color: UInt, val circle: Circle) : DrawRequest()
    class FillRect(val color: UInt, val rectangle: Rectangle) : DrawRequest()
}
