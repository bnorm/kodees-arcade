package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Segment
import kotlinx.serialization.json.Json

fun loadTrack(json: String): Track {
    val trackWidth = 1024.0
    val trackHeight = 768.0
    val jsonTrack = Json.decodeFromString<JsonTrack>(json)
    return Track(
        width = trackWidth,
        height = trackHeight,
        checkpoints = jsonTrack.checkpoints.map {
            Segment(
                start = Point(it.start.x, trackHeight - it.start.y),
                end = Point(it.end.x, trackHeight - it.end.y),
            )
        },
        positions = jsonTrack.pole_positions.map {
            Track.Position(
                location = Point(it.position.x, trackHeight - it.position.y),
                heading = Angle.ofDegrees(it.rotation.degrees.toDouble())
            )
        },
        laps = 25,
    )
}
