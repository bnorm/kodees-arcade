package dev.bnorm.arcade.rally.race

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.rally.Track
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.Serializable

interface Race {
    val events: ReceiveChannel<Event>
    suspend fun start()

    @Serializable
    sealed interface Event {
        @Serializable
        class Start(val track: Track) : Event

        @Serializable
        class Update(
            val time: Long,
            val racers: Map<String, Racer>,
        ) : Event {
            @Serializable
            class Racer(
                val x: Double,
                val y: Double,
                val heading: Angle,
            )
        }

        @Serializable
        data object Complete : Event

    }
}
