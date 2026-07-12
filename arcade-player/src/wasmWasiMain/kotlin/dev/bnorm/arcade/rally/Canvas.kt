package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment

interface Canvas {
    // TODO support fill vs stroke+width

    fun drawSegment(color: Color, segment: Segment)

    fun drawCircle(color: Color, circle: Circle)

    fun fillCircle(color: Color, circle: Circle)

    fun fillRect(color: Color, rectangle: Rectangle)

    fun drawRect(color: Color, rectangle: Rectangle) {
        val center = rectangle.center
        val half = Point(rectangle.width / 2, rectangle.height / 2)
        val cross = Point(rectangle.width / 2, -rectangle.height / 2)
        drawSegment(color, Segment(center + half, center + cross))
        drawSegment(color, Segment(center + half, center - cross))
        drawSegment(color, Segment(center - half, center - cross))
        drawSegment(color, Segment(center - half, center + cross))
    }
}
