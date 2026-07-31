package dev.bnorm.arcade.display.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toSize
import dev.bnorm.arcade.display.SCROLL_NORMALIZER
import dev.bnorm.arcade.display.internal.FixedSize
import dev.bnorm.arcade.display.internal.onKeyboard
import dev.bnorm.arcade.display.internal.rememberKeyboardState
import dev.bnorm.arcade.driver.Track
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
import dev.bnorm.arcade.geometry.toAbsolute
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toNormal
import dev.bnorm.arcade.geometry.toPoint
import dev.bnorm.arcade.geometry.toRelative
import dev.bnorm.arcade.geometry.toVector
import dev.bnorm.arcade.rally.TRACK_WIDTH
import kotlin.math.abs
import kotlin.math.sqrt

private enum class BuilderMode {
    /** Create a new track curve. */
    Curve,

    /** Create a new track straight. */
    Straight,

    /** Close the track with two final segments. */
    Close,

    /** Transform segments due to mouse move or scroll. */
    Transform,

    ;
}

private sealed class BuilderResult(
    val point: Point,
    val segment: Segment?,
) {
    class First(point: Point, segment: Segment?) : BuilderResult(point, segment)
    class Straight(point: Point, segment: Segment?) : BuilderResult(point, segment)
    class Curve(point: Point, segment: Segment?) : BuilderResult(point, segment)
    class Close(point: Point, segment: Segment?) : BuilderResult(point, segment)
}

@Composable
fun TrackBuilder(initialSize: IntSize, onSave: (Track) -> Unit, modifier: Modifier = Modifier) {
    var size by remember { mutableStateOf(initialSize.toSize()) }

    // the *second* checkpoint is the starting line
    // the first and second checkpoints define the starting grid and how many positions are possible
    var complete by remember { mutableStateOf(false) }
    val checkpoints = remember { mutableStateListOf<Segment>() }
    val positions by derivedStateOf {
        computePositions(checkpoints)
    }

    fun move(delta: Point) {
        val new = checkpoints.map { it + delta }
        checkpoints.clear()
        checkpoints.addAll(new)
    }

    fun scale(scrollDelta: Float, location: Point) {
        size = Size(
            width = (size.width + scrollDelta * size.width).coerceIn(1f, 32_766f),
            height = (size.height + scrollDelta * size.height).coerceIn(1f, 32_766f),
        )
        move(scrollDelta.toDouble() * location)
    }

    val keyboardState = rememberKeyboardState()
    var mouse by remember { mutableStateOf<Point?>(null) }
    var point by remember { mutableStateOf<Point?>(null) }

    val mode by derivedStateOf {
        when (keyboardState.pressed.singleOrNull()) {
            Key.C -> BuilderMode.Close
            Key.S -> BuilderMode.Straight
            Key.CtrlLeft, Key.CtrlRight -> BuilderMode.Transform
            else -> BuilderMode.Curve
        }
    }

    val builderResult by derivedStateOf {
        val mouse = mouse
        val point = point
        when (mode) {
            BuilderMode.Transform -> null
            else if (complete || mouse == null) -> null
            else if point != null -> computeFirstSegment(mouse, point)

            else if checkpoints.isEmpty() -> null

            BuilderMode.Close -> computeMiddleSegment(
                mouse = mouse,
                prev = checkpoints.last(),
                next = checkpoints.first()
            )

            BuilderMode.Straight -> computeStraightSegment(
                mouse = mouse,
                last = checkpoints.last()
            )

            BuilderMode.Curve -> computeCurveSegment(
                mouse = mouse,
                last = checkpoints.last()
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .focusable()
            .focusRequester(focusRequester)
            .onKeyboard(keyboardState)
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

                    else -> false
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                enabled = complete,
                onClick = {
                    focusRequester.requestFocus()

                    val bounds = TrackPath.of(checkpoints).bounds
                    val padding = TRACK_WIDTH
                    val delta = Point(bounds.minX - padding, bounds.minY - padding)
                    val boundedCheckpoints = checkpoints.map { it - delta }
                    val track = Track(
                        width = bounds.width + 2.0 * padding,
                        height = bounds.height + 2.0 * padding,
                        // Rotate checkpoints so the first defines the starting line.
                        checkpoints = List(checkpoints.size) { boundedCheckpoints[(it + 1) % checkpoints.size] },
                        positions = computePositions(boundedCheckpoints),
                    )
                    onSave(track)
                }
            ) {
                Text("Save")
            }

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

            Text(
                """
                Welcome to the track builder!
                To add track segments, click within the box.
                
                The first segment takes 3 clicks:
                1. To determine the starting location.
                2. To determine the starting direction.
                3. To determine the curvature of starting segment.
                
                To make sure the track remains a constant width,
                all segments are straight or perfectly circular.
                A preview segment will be shown as the mouse is moved.
                
                To remove a segment, press Ctrl+Z.
                To remove all segments, click the 'Clear' button.
                
                To lock the segment to be straight, hold S.
                To complete the track with 2 final segments, hold C.
                To resize the entire track, hold Ctrl and scroll.
                To move the entire track, hold Ctrl, click, and drag.
                
                When saving, the track is automatically sized and padded
                based on the track segments.
                
                Tips!
                1. Because drivers only receive checkpoints and there
                is not traction, it is very easy to cut corners. Break
                up longer corners into smaller segments!
                2. The first segment determines the starting grid. The
                bigger it is, the more drivers can participate!
                """.trimIndent()
            )
        }

        FixedSize(
            size = size.roundToIntSize(),
            density = Density(1f),
            modifier = Modifier
                .border(2.dp, Color.Black)
                .clipToBounds()
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(size) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        val location = event.changes.first().position.toPoint()
                                        mouse = if (!complete) location else null
                                    }

                                    PointerEventType.Exit -> {
                                        mouse = null
                                        point = null
                                    }

                                    PointerEventType.Move -> {
                                        val change = event.changes.first()
                                        val location = change.position.toPoint()
                                        mouse = if (!complete) location else null

                                        if (mode == BuilderMode.Transform && change.pressed) {
                                            move(change.position.toPoint() - change.previousPosition.toPoint())
                                        }
                                    }

                                    PointerEventType.Scroll -> {
                                        if (mode == BuilderMode.Transform) {
                                            val change = event.changes.first()
                                            scale(change.scrollDelta.y / SCROLL_NORMALIZER, change.position.toPoint())
                                        }
                                    }

                                    PointerEventType.Release -> {
                                        focusRequester.requestFocus()
                                        if (!complete && !event.buttons.areAnyPressed) {
                                            when (val result = builderResult) {
                                                null -> {}

                                                is BuilderResult.First -> {
                                                    result.segment?.let {
                                                        checkpoints.add(it)
                                                        point = null
                                                    }
                                                }

                                                is BuilderResult.Close -> {
                                                    result.segment?.let {
                                                        checkpoints.add(it)
                                                        complete = true
                                                    }
                                                }

                                                is BuilderResult.Curve,
                                                is BuilderResult.Straight,
                                                    -> {
                                                    result.segment?.let { checkpoints.add(it) }
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
                drawTrack(
                    checkpoints = checkpoints,
                    startingLine = checkpoints.getOrNull(1),
                    positions = positions,
                    complete = complete,
                )

                for (segment in checkpoints) {
                    drawLine(
                        color = Color.Blue,
                        start = segment.start.toOffset(),
                        end = segment.end.toOffset(),
                        strokeWidth = 4f,
                    )
                }

                drawBuilderResult(builderResult, checkpoints)

                val mouse = mouse
                if (mouse != null && builderResult == null) {
                    drawCircle(
                        color = Color.Green,
                        center = mouse.toOffset(),
                        radius = 4f,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBuilderResult(
    result: BuilderResult?,
    checkpoints: SnapshotStateList<Segment>
) {
    result ?: return

    if (result.segment != null) {
        drawSegment(Color.Green, result.segment)

        when (result) {
            is BuilderResult.First -> {}

            is BuilderResult.Straight -> {
                val previous = checkpoints.last()
                drawLinesBetween(Color.Green, previous, result.segment)
            }

            is BuilderResult.Curve -> {
                val previous = checkpoints.last()
                drawArcsBetween(Color.Green, previous, result.segment)
            }

            is BuilderResult.Close -> {
                val previous = checkpoints.last()
                val next = checkpoints.first()
                drawArcsBetween(Color.Green, previous, result.segment)
                drawArcsBetween(Color.Green, result.segment, next)
            }
        }
    }

    drawCircle(
        color = if (result.segment != null) Color.Green else Color.Red,
        center = result.point.toOffset(),
        radius = 4f,
    )
}

private fun computePositions(checkpoints: List<Segment>): List<Position> {
    if (checkpoints.size < 2) return emptyList()

    val first = checkpoints[0]
    val second = checkpoints[1]

    val separation = (TRACK_WIDTH - 2.0 * CAR_WIDTH) / 3.0
    val dist = separation + CAR_WIDTH
    val padding = separation + CAR_WIDTH / 2

    return buildList {
        fun buildForStraight() {
            val vector = first.center.toVector(second.center)
            val total = vector.magnitude

            val distVector = Vector(vector.angle, dist).toPoint()
            val offsetVector = Vector(vector.angle + Angle.QUARTER_CIRCLE, dist / 2.0).toPoint()

            val start = second.center + Vector(vector.angle, padding)
            val heading = (vector.angle + Angle.HALF_CIRCLE).toAbsolute()
            repeat((total / dist).toInt()) {
                val point = start + distVector * it.toDouble()
                add(Position(point + offsetVector, heading))
                add(Position(point - offsetVector + distVector / 2.0, heading))
            }
        }

        fun buildForArc(center: Point, innerRadius: Double, outerRadius: Double) {
            val start = center.angleTo(second.start)
            val total = (start - center.angleTo(first.start)).toRelative()
            val sign = sign(total)

            val paddingAngle = acos(1 - (padding * padding) / (2 * innerRadius * innerRadius)) * sign
            val distAngle = acos(1 - (dist * dist) / (2 * innerRadius * innerRadius)) * sign

            val available = (total / distAngle).toInt()
            repeat(abs(available)) {
                val angleInner = start - paddingAngle - distAngle * it.toDouble()
                add(
                    Position(
                        location = center + Vector(angleInner, innerRadius),
                        heading = angleInner + sign * Angle.QUARTER_CIRCLE
                    )
                )
                val angleOuter = angleInner - distAngle / 2.0
                add(
                    Position(
                        location = center + Vector(angleOuter, outerRadius),
                        heading = angleOuter + sign * Angle.QUARTER_CIRCLE
                    )
                )
            }
        }

        val center = first.toLine().intersect(second.toLine())
        if (center == null) {
            buildForStraight()
        } else {
            // TODO use sign to determine start vs end?
            val innerRadius = minOf(center.distanceTo(first.start), center.distanceTo(first.end)) + padding
            val outerRadius = innerRadius + dist

            if (minOf(innerRadius, outerRadius) > VISUALLY_STRAIGHT_ENOUGH) {
                buildForStraight()
            } else {
                buildForArc(center, innerRadius, outerRadius)
            }
        }
    }
}

private fun computeFirstSegment(
    mouse: Point,
    point: Point
): BuilderResult.First {
    val angle = point.angleTo(mouse)
    val delta = Vector(angle - Angle.QUARTER_CIRCLE, TRACK_WIDTH / 2).toPoint()
    return BuilderResult.First(point, Segment(point + delta, point - delta))
}

private fun computeCurveSegment(
    mouse: Point,
    last: Segment,
): BuilderResult {
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
//    distance = minOf(distance, 250.0)

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
        BuilderResult.Curve(segment.center, segment)
    } else {
        BuilderResult.Curve(
            point = segmentCenter + Vector(angle, magnitude = 2 * radius * cosAlpha),
            segment = null
        )
    }
}

private fun computeStraightSegment(
    mouse: Point,
    last: Segment
): BuilderResult {
    val segmentCenter = last.center
    val segmentVector = (last.end - last.start).toVector()

    val deltaMouseAngle = (segmentCenter.angleTo(mouse) - segmentVector.angle).toRelative()
    // Must be to the right (negative) of the segment vector.
    if (deltaMouseAngle > Angle.ZERO) return BuilderResult.Straight(segmentCenter, segment = null)

    val nearest = mouse.nearest(last.toLine().toNormal(segmentCenter))
    val distance = segmentCenter.distanceTo(nearest)

    // Limit direct distance between segments.
    // TODO configurable?
//    distance = minOf(distance, 250.0)

    // TODO there's a way to reduce the number of cos/sin usage here
    val deltaVector = Vector(segmentVector.angle - Angle.QUARTER_CIRCLE, distance)
    val segment = Segment(start = last.start + deltaVector, end = last.end + deltaVector)
    return BuilderResult.Straight(segment.center, segment)
}

private fun computeMiddleSegment(
    mouse: Point?,
    prev: Segment,
    next: Segment,
): BuilderResult {
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
        BuilderResult.Close(segment.center, segment)
    } else {
        BuilderResult.Close(
            point = prevCenter + Vector(angle, magnitude = 2.0 * radius1 * cosAlpha1),
            segment = null
        )
    }
}

context(scope: PointerInputScope)
private fun Offset.toPoint(): Point {
    return Point(x.toDouble(), scope.size.height - y.toDouble())
}
