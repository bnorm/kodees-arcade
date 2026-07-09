package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RaceCreateRequest(
    val trackId: TrackId,
    val positions: List<Position>,
    val laps: Int,
) {
    @Serializable
    class Position(
        val driverId: DriverId,
        val version: Version,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Position

            if (driverId != other.driverId) return false
            if (version != other.version) return false

            return true
        }

        override fun hashCode(): Int {
            var result = driverId.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }
    }
}
