package dev.bnorm.arcade.web.route.races

import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.api.Version
import kotlin.time.Instant

class RaceModel(
    val id: RaceId,
    val startTime: Instant?,
    val endTime: Instant?,
    val drivers: List<Driver>,
    val track: Track,
) {
    class Driver(
        val id: DriverId,
        val name: String,
        val version: Version,
        val result: Double?,
    )

    class Track(
        val id: TrackId,
        val name: String,
    )
}

fun RaceResponse.toModel(track: TrackResponse?): RaceModel {
    require(track == null || trackId == track.id)
    return RaceModel(
        id = id,
        startTime = startTime,
        endTime = endTime,
        drivers = drivers.map { version ->
            RaceModel.Driver(
                id = version.driverId,
                name = version.name,
                version = version.version,
                result = version.result,
            )
        },
        track = RaceModel.Track(
            id = trackId,
            name = track?.name ?: "N/A"
        )
    )
}
