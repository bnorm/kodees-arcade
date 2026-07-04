package dev.bnorm.arcade.service.api

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
class RaceResponse(
    val id: RaceId,
    val trackId: TrackId,
    val startTime: Instant?,
    val endTime: Instant?,
    val drivers: List<Driver>,
) {
    @Serializable
    class Driver(
        val id: DriverId,
        val name: String,
        val version: Version,
    )
}
