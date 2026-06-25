package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Segment
import kotlinx.serialization.Serializable

@Serializable
class Track(
    val width: Double,
    val height: Double,
    val checkpoints: List<Segment>,
    val positions: List<Position>,
    val laps: Int,
) {
    @Serializable
    class Position(
        val location: Point,
        val heading: Angle,
    )
}
