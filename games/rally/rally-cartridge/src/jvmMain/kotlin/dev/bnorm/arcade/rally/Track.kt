package dev.bnorm.arcade.rally

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
class Track(
    val scale: Double,
    val checkpoints: List<Line>,
    val obstacles: List<Obstacle>,
    val boundary_offsets: BoundaryOffsets,
    val max_cars: Int,
    val obstacle_types: List<String>,
    val starting_line: Line,
    val pole_positions: List<PolePosition>,
) {
    @Serializable
    class Point(
        val x: Double,
        val y: Double,
    )

    @Serializable
    class Rotation(
        val degrees: Int,
        val radians: Int,
    )

    @Serializable
    class Line(
        val start: Point,
        val end: Point,
    ) {
        val length: Double = run {
            val dx = start.x - end.x
            val dy = start.y - end.y
            sqrt(dx * dx + dy * dy)
        }
    }

    @Serializable
    class Obstacle(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
    )

    @Serializable
    class BoundaryOffsets(
        val top: Int,
        val right: Int,
        val bottom: Int,
        val left: Int,
    )

    @Serializable
    class PolePosition(
        val position: Point,
        val rotation: Rotation,
    )
}

fun Track.Obstacle.toTopLeft(): Offset {
    return Offset(x.toFloat(), y.toFloat())
}

fun Track.Obstacle.toSize(): Size {
    return Size(w.toFloat(), h.toFloat())
}

fun Track.Point.toOffset(): Offset {
    return Offset(x.toFloat(), y.toFloat())
}

fun Track.Line.toCenter(): Track.Point {
    return Track.Point(
        x = start.x + (end.x - start.x) / 2,
        y = start.y + (end.y - start.y) / 2,
    )
}
