package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable

@Serializable
class Track(
    val width: Double,
    val height: Double,
    val checkpoints: List<Segment>,
    val positions: List<Position>
) {
    @Serializable
    class Position(
        val location: Point,
        val heading: Angle,
    )
}
