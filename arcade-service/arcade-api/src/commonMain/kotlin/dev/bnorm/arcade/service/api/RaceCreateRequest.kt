package dev.bnorm.arcade.service.api

import kotlinx.serialization.Serializable

@Serializable
class RaceCreateRequest(
    val trackId: TrackId,
    val racers: List<Racer>,
) {
    @Serializable
    class Racer(
        val id: RacerId,
        val version: Version,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Racer

            if (id != other.id) return false
            if (version != other.version) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }
    }
}
