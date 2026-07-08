package dev.bnorm.arcade.web.route.tracks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.Vector
import dev.bnorm.arcade.geometry.acos
import dev.bnorm.arcade.geometry.asin
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.cos
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.nearest
import dev.bnorm.arcade.geometry.plus
import dev.bnorm.arcade.geometry.sign
import dev.bnorm.arcade.geometry.sin
import dev.bnorm.arcade.geometry.times
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toNormal
import dev.bnorm.arcade.geometry.toPoint
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.geometry.toVector
import dev.bnorm.arcade.rally.FixedSize
import dev.bnorm.arcade.rally.Track
import kotlin.math.abs
import kotlin.math.sqrt

private enum class AddMode {
    Curve,
    Straight,
    Close,
}

private sealed class SegmentResult(
    val point: Point,
) {
    class First(point: Point, val first: Segment, val second: Segment?) : SegmentResult(point)
    class Straight(point: Point, val segment: Segment?) : SegmentResult(point)
    class Curve(point: Point, val segment: Segment?) : SegmentResult(point)
    class Close(point: Point, val segment: Segment?) : SegmentResult(point)
}

@Composable
fun TrackBuilder(size: IntSize, onSave: (Track) -> Unit, modifier: Modifier = Modifier) {
    // the *second* checkpoint is the starting line
    // the first and second checkpoints define the starting grid and how many positions are possible
    var complete by remember { mutableStateOf(false) }
    val checkpoints = remember { mutableStateListOf<Segment>() }

    var mouse by remember { mutableStateOf<Point?>(null) }
    var addMode by remember { mutableStateOf(AddMode.Curve) }

    var point by remember { mutableStateOf<Point?>(null) }
    val segment by derivedStateOf {
        when {
            complete -> null

            addMode == AddMode.Close && checkpoints.isNotEmpty() -> computeMiddleSegment(
                mouse = mouse,
                prev = checkpoints.last(),
                next = checkpoints.first()
            )

            addMode == AddMode.Straight -> computeStraightSegment(
                mouse = mouse,
                point = point,
                last = checkpoints.lastOrNull()
            )

            else -> computeCurveSegment(
                mouse = mouse,
                point = point,
                last = checkpoints.lastOrNull()
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent {
                when (it.key) {
                    Key.Z if it.isCtrlPressed && it.type == KeyEventType.KeyUp -> {
                        if (complete) {
                            checkpoints.removeLast()
                            complete = false
                        } else if (checkpoints.isEmpty()) {
                            point = null
                        } else {
                            checkpoints.removeLast()
                        }
                        true
                    }

                    Key.C -> {
                        addMode = if (it.type == KeyEventType.KeyDown) AddMode.Close else AddMode.Curve
                        true
                    }

                    Key.S -> {
                        addMode = if (it.type == KeyEventType.KeyDown) AddMode.Straight else AddMode.Curve
                        true
                    }

                    else -> false
                }
            }
    ) {
        Row {
            Button(
                onClick = {
                    focusRequester.requestFocus()
                    point = null
                    checkpoints.clear()
                    complete = false
                }
            ) {
                Text("Clear")
            }

            Button(
                enabled = complete,
                onClick = {
                    focusRequester.requestFocus()
                    val track = Track(
                        width = size.width.toDouble(),
                        height = size.height.toDouble(),
                        // Rotate checkpoints so the first defines the starting line.
                        checkpoints = List(checkpoints.size) { checkpoints[(it + 1) % checkpoints.size] },
                        positions = computePositions(checkpoints),
                        laps = 25,
                    )
                    onSave(track)
                }
            ) {
                Text("Save")
            }
        }

        FixedSize(
            size = size,
            density = Density(1f),
        ) {
//            SpaFrancorchamps(Modifier.fillMaxSize())
            Canvas(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.Black)
                    .pointerInput(size) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                    }

                                    PointerEventType.Exit -> {
                                        mouse = null
                                        point = null
                                    }

                                    PointerEventType.Move -> {
                                        mouse = if (!complete) event.changes.first().position.toPoint() else null
                                    }

                                    PointerEventType.Release -> {
                                        focusRequester.requestFocus()
                                        if (!complete) {
                                            when (val result = segment) {
                                                null -> {}

                                                is SegmentResult.First -> {
                                                    checkpoints.add(result.first)
                                                    result.second?.let { checkpoints.add(it) }
                                                    point = null
                                                }

                                                is SegmentResult.Straight -> {
                                                    result.segment?.let { checkpoints.add(it) }
                                                }

                                                is SegmentResult.Curve -> {
                                                    result.segment?.let { checkpoints.add(it) }
                                                }

                                                is SegmentResult.Close -> {
                                                    result.segment?.let {
                                                        checkpoints.add(it)
                                                        complete = true
                                                    }
                                                }
                                            }

                                            if (checkpoints.isEmpty() && point == null) {
                                                point = event.changes.first().position.toPoint()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                drawPath(centerLine, color = Color.Gray, style = Stroke(width = TRACK_WIDTH.toFloat()))

                for (segment in checkpoints) {
                    drawLine(
                        color = Color.Blue,
                        start = segment.start.toOffset(),
                        end = segment.end.toOffset(),
                        strokeWidth = 4f,
                    )
                }

                val result = segment
                if (result != null) {
                    val valid = when (result) {
                        is SegmentResult.First -> {
                            drawSegment(Color.Green, result.first)
                            if (result.second != null) {
                                drawSegment(Color.Green, result.second)
                                drawLinesBetween(Color.Green, result.first, result.second)
                            }
                            true
                        }

                        is SegmentResult.Straight -> {
                            val next = result.segment
                            if (next != null) {
                                val previous = checkpoints.last()
                                drawSegment(Color.Green, next)
                                drawLinesBetween(Color.Green, previous, next)
                            }
                            next != null
                        }

                        is SegmentResult.Curve -> {
                            val next = result.segment
                            if (next != null) {
                                val previous = checkpoints.last()
                                drawSegment(Color.Green, next)
                                drawArcsBetween(Color.Green, previous, next)
                            }
                            next != null
                        }

                        is SegmentResult.Close -> {
                            val middle = result.segment
                            if (middle != null) {
                                val previous = checkpoints.last()
                                val next = checkpoints.first()
                                drawSegment(Color.Green, middle)
                                drawArcsBetween(Color.Green, previous, middle)
                                drawArcsBetween(Color.Green, middle, next)
                            }
                            middle != null
                        }
                    }

                    drawCircle(
                        color = if (valid) Color.Green else Color.Red,
                        center = result.point.toOffset(),
                        radius = 4f,
                    )
                }

                val point = point
                if (point != null) {
                    drawCircle(
                        color = Color.Green,
                        center = point.toOffset(),
                        radius = 4f,
                    )
                }

                val mouse = mouse
                if (mouse != null) {
                    val offset = mouse.toOffset()
                    drawCircle(
                        color = if (point != null || segment != null || checkpoints.isNotEmpty()) Color.Green else Color.Red,
                        center = offset,
                        radius = 4f,
                    )
                }
            }
        }
    }
}

private fun computePositions(checkpoints: SnapshotStateList<Segment>): List<Position> {
    if (checkpoints.size < 2) return emptyList()

    return buildList {
        val first = checkpoints[0]
        val second = checkpoints[1]

        val center = first.toLine().intersect(second.toLine())
        if (center != null) {
            val separation = (TRACK_WIDTH - 2.0 * CAR_WIDTH) / 3.0
            val dist = separation + CAR_WIDTH
            val padding = separation + CAR_WIDTH / 2

            val start = center.angleTo(second.start)
            val total = (start - center.angleTo(first.start)).toRelative()
            val sign = sign(total)

            // TODO use sign to determine start vs end
            val innerRadius = minOf(center.distanceTo(first.start), center.distanceTo(first.end)) + padding
            val outerRadius = innerRadius + dist

            val paddingAngle = acos(1 - (padding * padding) / (2 * innerRadius * innerRadius)) * sign
            val distAngle = acos(1 - (dist * dist) / (2 * innerRadius * innerRadius)) * sign

            val available = (total / distAngle).toInt()
            repeat(abs(available)) {
                val angle = start - paddingAngle - distAngle * it.toDouble()
                add(
                    Position(
                        location = (center + Vector(angle, innerRadius)),
                        heading = angle + sign * Angle.QUARTER_CIRCLE,
                    )
                )
                add(
                    Position(
                        location = (center + Vector(angle, outerRadius)),
                        heading = angle + sign * Angle.QUARTER_CIRCLE,
                    )
                )
            }
        }
    }
}

private const val TRACK_WIDTH = 90.0
private const val CAR_WIDTH = 20.0

private fun computeCurveSegment(
    mouse: Point?,
    point: Point?,
    last: Segment?
): SegmentResult? {
    if (mouse == null) return null

    if (last != null) {
        val segmentCenter = last.center
        val segmentVector = (last.end - last.start).toVector()
        val vector = (mouse - segmentCenter).toVector()

        // Angle to the right (negative) of the segment vector.
        var alpha = (vector.angle - segmentVector.angle).toRelative()
        // Distance from segment center to the mouse.
        var distance = vector.magnitude

        // Limit angle and distance if the angle is to the left (positive) of the segment.
        // Also limit distance, so it's the distance along the segment line,
        // rather than distance to the mouse.
        // TODO try and remove the weird 0.0001 adjustment
        if (alpha > Angle.QUARTER_CIRCLE) {
            alpha = -Angle.HALF_CIRCLE + Angle.ofRadians(0.0001)
            distance = segmentCenter.distanceTo(mouse.nearest(last.toLine()))
        } else if (alpha > Angle.ZERO) {
            alpha = Angle.ZERO - Angle.ofRadians(0.0001)
            distance = segmentCenter.distanceTo(mouse.nearest(last.toLine()))
        }

        // Limit direct distance between segments.
        // TODO configurable?
        distance = minOf(distance, 250.0)

        // 'vector.magnitude' is the base of an isosceles triangle with base angle of 'alpha'.
        // Therefore, cos(alpha) = opposite / hypotenuse, where:
        // * hypotenuse = is the radius of the circle through both segment center and mouse.
        // * opposite = 'vector.magnitude' / 2.
        val cosAlpha = cos(Angle.HALF_CIRCLE - alpha)
        val radius = (distance / 2.0) / cosAlpha
        val angle = segmentVector.angle + alpha

        // Reverse the calculation with different radii to create a new segment,
        // which shares the same circle center point for its start and end point arcs.
        val halfWidth = segmentVector.magnitude / 2.0
        return if (abs(radius) > halfWidth) {
            // TODO there's a way to reduce the number of cos/sin usage here
            val segment = Segment(
                start = last.start + Vector(angle, 2 * (radius - halfWidth) * cosAlpha),
                end = last.end + Vector(angle, 2 * (radius + halfWidth) * cosAlpha)
            )
            SegmentResult.Curve(segment.center, segment)
        } else {
            SegmentResult.Curve(
                point = segmentCenter + Vector(angle, magnitude = 2 * radius * cosAlpha),
                segment = null
            )
        }
    } else if (point != null) {
        val delta = Vector(point.angleTo(mouse), TRACK_WIDTH / 2).toPoint()
        return SegmentResult.First(point, Segment(point - delta, point + delta), second = null)
    } else {
        return null
    }
}

private fun computeStraightSegment(
    mouse: Point?,
    point: Point?,
    last: Segment?
): SegmentResult? {
    if (mouse == null) return null

    if (last != null) {
        val segmentCenter = last.center
        val segmentVector = (last.end - last.start).toVector()

        val deltaMouseAngle = (segmentCenter.angleTo(mouse) - segmentVector.angle).toRelative()
        // Must be to the right (negative) of the segment vector.
        if (deltaMouseAngle > Angle.ZERO) return SegmentResult.Straight(segmentCenter, segment = null)

        val nearest = mouse.nearest(last.toLine().toNormal(segmentCenter))
        var distance = segmentCenter.distanceTo(nearest)

        // Limit direct distance between segments.
        // TODO configurable?
        distance = minOf(distance, 250.0)

        // TODO there's a way to reduce the number of cos/sin usage here
        val deltaVector = Vector(segmentVector.angle - Angle.QUARTER_CIRCLE, distance)
        val segment = Segment(start = last.start + deltaVector, end = last.end + deltaVector)
        return SegmentResult.Straight(segment.center, segment)
    } else if (point != null) {
        val angle = point.angleTo(mouse)
        var distance = point.distanceTo(mouse)

        // Limit direct distance between segments.
        // TODO configurable?
        distance = minOf(distance, 250.0)
        val secondCenter = point + Vector(angle, distance)

        val delta = Vector(angle - Angle.QUARTER_CIRCLE, TRACK_WIDTH / 2).toPoint()
        return SegmentResult.First(
            secondCenter,
            Segment(point + delta, point - delta),
            Segment(secondCenter + delta, secondCenter - delta),
        )
    } else {
        return null
    }
}

private fun computeMiddleSegment(
    mouse: Point?,
    prev: Segment,
    next: Segment,
): SegmentResult {
    val prevVector = (prev.end - prev.start).toVector()
    val prevCenter = prev.center

    val nextAngle = next.start.angleTo(next.end)
    val nextCenter = next.center

    val pathVector = (nextCenter - prevCenter).toVector()
    val dist = pathVector.magnitude

    val beta1 = (pathVector.angle - (prevVector.angle + Angle.HALF_CIRCLE)).toRelative()
    val beta2 = (nextAngle - pathVector.angle).toRelative()
    val pathAcuteAngle = (Angle.HALF_CIRCLE + beta1 + beta2) / 2.0

    // Use Law of Cosines to determine the edges.
    val cosPathAcuteAngle = cos(pathAcuteAngle)
    val dist1: Double
    val dist2: Double
    when {
        mouse == null -> {
            // Assume equidistant.
            dist1 = sqrt((dist * dist) / (2.0 - 2.0 * cosPathAcuteAngle))
            dist2 = dist1
        }

        else -> {
            // Use the mouse distance along the direct segment from center-to-center
            // to determine the ratio of distances.
            val nearest = mouse.nearest(Segment(prevCenter, nextCenter))
            val ratio = prevCenter.distanceTo(nearest) / dist
            if (ratio < 0.5) {
                val p = 2.0 * ratio // 0..1
                dist2 = sqrt((dist * dist) / (p * p + 1.0 - 2.0 * p * cosPathAcuteAngle))
                dist1 = p * dist2
            } else {
                val p = 2.0 - 2.0 * ratio // 0..1
                dist1 = sqrt((dist * dist) / (p * p + 1.0 - 2.0 * p * cosPathAcuteAngle))
                dist2 = p * dist1
            }
        }
    }

    // Use Law of Sines to determine the other angles of the triangle.
    val sinPathAcuteAngle = sin(pathAcuteAngle)
    val alpha1 = asin(sinPathAcuteAngle * dist2 / dist) + beta1
    val alpha2 = asin(sinPathAcuteAngle * dist1 / dist) + beta2

    // TODO check alphas to make sure they don't go "backwards"
    //  not sure if they should be negative or positive...

    val cosAlpha1 = cos(alpha1)
    val radius1 = (dist1 / 2.0) / cosAlpha1
    val radius2 = (dist2 / 2.0) / cos(alpha2)
    val angle = prevVector.angle + alpha1 + Angle.HALF_CIRCLE

    val halfWidth = prevVector.magnitude / 2.0
    return if (abs(radius1) > halfWidth && abs(radius2) > halfWidth) {
        // TODO there's a way to reduce the number of cos/sin usage here
        val segment = Segment(
            start = prev.start + Vector(angle, 2.0 * (radius1 - halfWidth) * cosAlpha1),
            end = prev.end + Vector(angle, 2.0 * (radius1 + halfWidth) * cosAlpha1)
        )
        SegmentResult.Close(segment.center, segment)
    } else {
        SegmentResult.Close(
            point = prevCenter + Vector(angle, magnitude = 2.0 * radius1 * cosAlpha1),
            segment = null
        )
    }
}

private fun DrawScope.drawArcsBetween(color: Color, start: Segment, end: Segment) {
    val center = start.toLine().intersect(end.toLine())
    if (center == null) {
        // Parallel segments, so just draw straight lines.
        drawLinesBetween(color, start, end)
    } else {
        val startAngle = center.angleTo(start.start)
        val endAngle = center.angleTo(end.start)
        val sweepAngle = (endAngle - startAngle).toRelative()

        drawArc(
            color = color,
            center = center,
            radius = center.distanceTo(end.start),
            startAngle = startAngle,
            sweepAngle = sweepAngle
        )
        drawArc(
            color = color,
            center = center,
            radius = center.distanceTo(end.end),
            startAngle = startAngle,
            sweepAngle = sweepAngle
        )
    }
}

private fun DrawScope.drawLinesBetween(color: Color, start: Segment, end: Segment) {
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

fun DrawScope.drawArc(
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

fun DrawScope.drawSegment(
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

context(scope: PointerInputScope)
fun Offset.toPoint(): Point {
    return Point(x.toDouble(), scope.size.height - y.toDouble())
}

context(scope: DrawScope)
fun Point.toOffset(): Offset {
    return Offset(x.toFloat(), scope.size.height - y.toFloat())
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
