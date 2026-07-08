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
import dev.bnorm.arcade.geometry.toPoint
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.geometry.toVector
import dev.bnorm.arcade.rally.FixedSize
import dev.bnorm.arcade.rally.Track
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun TrackBuilder(size: IntSize, onSave: (Track) -> Unit, modifier: Modifier = Modifier) {
    // the *second* checkpoint is the starting line
    // the first and second checkpoints define the starting grid and how many positions are possible

    var mouse by remember { mutableStateOf<Point?>(null) }
    var point by remember { mutableStateOf<Point?>(null) }
    var segment by remember { mutableStateOf<Segment?>(null) }
    val checkpoints = remember { mutableStateListOf<Segment>() }
    var complete by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent {
                (it.type == KeyEventType.KeyUp) && when (it.key) {
                    Key.Z if it.isCtrlPressed -> {
                        if (complete) {
                            checkpoints.removeLast()
                            complete = false
                        } else if (checkpoints.isEmpty()) {
                            point = null
                            segment = null
                        } else {
                            checkpoints.removeLast()
                            segment = computeNextSegment(mouse, point, checkpoints.lastOrNull())
                        }
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
    ) {
        Row {
            Button(
                onClick = {
                    focusRequester.requestFocus()
                    point = null
                    segment = null
                    checkpoints.clear()
                    complete = false
                }
            ) {
                Text("Clear")
            }

            Button(
                enabled = !complete,
                onClick = {
                    focusRequester.requestFocus()
                    val last = computeMiddleSegment(mouse, checkpoints.last(), checkpoints.first())
                    if (last != null) {
                        checkpoints.add(last)
                        point = null
                        segment = null
                        complete = true
                    }
                }
            ) {
                Text("Close")
            }

            Button(
                enabled = complete,
                onClick = {
                    focusRequester.requestFocus()
                    // TODO rotate checkpoints so the first defines the starting line
                    val track = Track(
                        width = size.width.toDouble(),
                        height = size.height.toDouble(),
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
                                        segment = null
                                    }

                                    PointerEventType.Move -> {
                                        if (!complete) {
                                            val location = event.changes.first().position.toPoint(size)
                                            mouse = location
                                            segment = computeNextSegment(location, point, checkpoints.lastOrNull())
                                        } else {
                                            mouse = null
                                        }
                                    }

                                    PointerEventType.Release -> {
                                        focusRequester.requestFocus()
                                        if (!complete) {
                                            val location = event.changes.first().position.toPoint(size)

                                            segment?.let {
                                                checkpoints.add(it)
                                                point = null
                                            }

                                            if (checkpoints.isEmpty() && point == null) {
                                                point = location
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val centerPath = checkpoints.toPaths(this.size, complete = complete)
                val dashLength = 25f
                drawPath(centerPath, color = Color.Black, style = Stroke(width = TRACK_WIDTH.toFloat() + 6f))
                drawPath(
                    centerPath,
                    color = Color.White,
                    style = Stroke(
                        width = TRACK_WIDTH.toFloat() + 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength), phase = dashLength)
                    )
                )
                drawPath(centerPath, color = Color.Black, style = Stroke(width = TRACK_WIDTH.toFloat() + 1f))
                drawPath(centerPath, color = Color.Gray, style = Stroke(width = TRACK_WIDTH.toFloat()))

                for (segment in checkpoints) {
                    drawLine(
                        color = Color.Blue,
                        start = segment.start.toOffset(size),
                        end = segment.end.toOffset(size),
                        strokeWidth = 4f,
                    )
                }

                val segment = segment
                if (segment != null) {
                    drawLine(
                        color = Color.Green,
                        start = segment.start.toOffset(size),
                        end = segment.end.toOffset(size),
                        strokeWidth = 4f,
                    )
                    val previous = checkpoints.lastOrNull()
                    if (previous != null) {
                        drawArcsBetween(Color.Green, previous, segment)
                    }
                }

                val point = point
                if (point != null) {
                    drawCircle(
                        color = Color.Green,
                        center = point.toOffset(size),
                        radius = 4f,
                    )
                }

                val mouse = mouse
                if (mouse != null) {
                    val offset = mouse.toOffset(size)
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

fun computeNextSegment(
    mouse: Point?,
    point: Point?,
    last: Segment?
): Segment? {
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

        // Reverse the calculation with different radii to create a new segment,
        // which shares the same circle center point for its start and end point arcs.
        val halfWidth = segmentVector.magnitude / 2.0
        return if (abs(radius) > halfWidth) {
            // TODO there's a way to reduce the number of cos/sin usage here
            Segment(
                start = last.start + Vector(
                    segmentVector.angle + alpha,
                    2 * (radius - halfWidth) * cosAlpha
                ),
                end = last.end + Vector(
                    segmentVector.angle + alpha,
                    2 * (radius + halfWidth) * cosAlpha
                )
            )
        } else {
            null
        }
    } else if (point != null) {
        val delta = Vector(point.angleTo(mouse), TRACK_WIDTH / 2).toPoint()
        return Segment(point - delta, point + delta)
    } else {
        return null
    }
}

fun computeMiddleSegment(
    mouse: Point?,
    prev: Segment,
    next: Segment,
): Segment? {
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

    val cosAlpha1 = cos(alpha1)
    val radius1 = (dist1 / 2.0) / cosAlpha1
    val radius2 = (dist2 / 2.0) / cos(alpha2)

    val halfWidth = prevVector.magnitude / 2.0
    return if (abs(radius1) > halfWidth && abs(radius2) > halfWidth) {
        // TODO there's a way to reduce the number of cos/sin usage here
        Segment(
            start = prev.start + Vector(
                prevVector.angle + alpha1 + Angle.HALF_CIRCLE,
                2.0 * (radius1 - halfWidth) * cosAlpha1,
            ),
            end = prev.end + Vector(
                prevVector.angle + alpha1 + Angle.HALF_CIRCLE,
                2.0 * (radius1 + halfWidth) * cosAlpha1,
            )
        )
    } else {
        // TODO we did something wrong... this should always be possible?
        null
    }
}

private fun DrawScope.drawArcsBetween(color: Color, start: Segment, end: Segment) {
    val center = start.toLine().intersect(end.toLine())
    if (center == null) {
        // Parallel lines
        // TODO can we just draw straight lines?
        return
    }

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

fun Offset.toPoint(size: IntSize): Point {
    return Point(x.toDouble(), size.height - y.toDouble())
}

fun Point.toOffset(size: IntSize): Offset {
    return Offset(x.toFloat(), size.height - y.toFloat())
}

fun List<Segment>.toPaths(size: Size, complete: Boolean = true): Path {
    val centerLine = Path()
    if (this.isEmpty()) return centerLine

    fun pathToNext(prev: Segment, next: Segment) {
        val intersect = prev.toLine().intersect(next.toLine())
        if (intersect == null) {
            // TODO parallel lines?
            val center = next.center
            centerLine.lineTo(center.x.toFloat(), size.height - center.y.toFloat())
            TODO("parallel lines")
        } else {
            val startAngle = intersect.angleTo(prev.start)
            val endAngle = intersect.angleTo(next.start)
            val sweepAngle = (endAngle - startAngle).toRelative()
            val radius = intersect.distanceTo(prev.center)

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
