package dev.bnorm.arcade.service.api

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
class RaceResponse(
    val id: RaceId,
    val trackId: TrackId,
    val startTime: Instant?,
    val endTime: Instant?,
    val positions: List<Position>,
) {
    @Serializable
    class Position(
        val position: Int,
        val driverId: DriverId,
        val name: String,
        val version: Version,
    )
}
