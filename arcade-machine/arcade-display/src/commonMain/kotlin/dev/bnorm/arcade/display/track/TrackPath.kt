package dev.bnorm.arcade.display.track

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Arc
import dev.bnorm.arcade.geometry.Boundable
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.abs
import dev.bnorm.arcade.geometry.center
import dev.bnorm.arcade.geometry.intersect
import dev.bnorm.arcade.geometry.merge
import dev.bnorm.arcade.geometry.toLine
import dev.bnorm.arcade.geometry.toRelative

class TrackPath private constructor(
    val parts: List<TrackPart>
) : Boundable {
    companion object {
        fun of(checkpoints: List<Segment>, complete: Boolean = true): TrackPath {
            fun pathToNext(prev: Segment, next: Segment): TrackPart {
                val intersect = prev.toLine().intersect(next.toLine())
                val prevCenter = prev.center
                val nextCenter = next.center

                if (intersect == null) {
                    // Parallel segments, so just draw straight lines.
                    return TrackPart.Straight(Segment(prevCenter, nextCenter))
                } else {
                    val radius = intersect.distanceTo(prevCenter)
                    if (radius > VISUALLY_STRAIGHT_ENOUGH) {
                        // Basically parallel segments, so just draw straight lines.
                        return TrackPart.Straight(Segment(prevCenter, nextCenter))
                    } else {
                        val startAngle = intersect.angleTo(prevCenter)
                        val endAngle = intersect.angleTo(nextCenter)
                        val sweepAngle = (endAngle - startAngle).toRelative()
                        val arc = Arc(
                            pivot = intersect,
                            radius = radius,
                            start = if (sweepAngle < Angle.ZERO) endAngle else startAngle,
                            sweep = abs(sweepAngle),
                        )

                        return TrackPart.Curve(
                            arc = arc,
                            start = prevCenter,
                            end = nextCenter
                        )
                    }
                }
            }

            return TrackPath(
                parts = buildList {
                    var previous: Segment? = null
                    for (current in checkpoints) {
                        if (previous != null) {
                            add(pathToNext(previous, current))
                        }
                        previous = current
                    }

                    if (complete && previous != null) {
                        add(pathToNext(previous, checkpoints.first()))
                    }
                }
            )
        }
    }

    override val bounds = run {
        val rect = parts.merge().bounds
        Rectangle(
            minX = rect.minX - TRACK_WIDTH / 2.0,
            minY = rect.minY - TRACK_WIDTH / 2.0,
            maxX = rect.maxX + TRACK_WIDTH / 2.0,
            maxY = rect.maxY + TRACK_WIDTH / 2.0,
        )
    }
}

sealed class TrackPart : Boundable {
    abstract val start: Point
    abstract val end: Point

    class Straight(
        val segment: Segment,
    ) : TrackPart() {
        override val start: Point get() = segment.start
        override val end: Point get() = segment.end

        // TODO expand bounds based on left and right side
        override val bounds: Rectangle get() = segment.bounds
    }

    class Curve(
        val arc: Arc,
        override val start: Point,
        override val end: Point,
    ) : TrackPart() {
        // TODO expand bounds based on inner and outer lines
        override val bounds: Rectangle get() = arc.bounds
    }
}
