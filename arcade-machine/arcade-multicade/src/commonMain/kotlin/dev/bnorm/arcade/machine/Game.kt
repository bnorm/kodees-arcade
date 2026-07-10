package dev.bnorm.arcade.machine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.rally.Track
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface Game {
    val events: ReceiveChannel<Event>
    suspend fun start()

    @Serializable
    sealed interface Event {
        @Serializable
        @SerialName("Start")
        class Start(val track: Track, val drivers: List<String>) : Event

        @Serializable
        @SerialName("Update")
        class Update(
            val drivers: List<Driver>,
        ) : Event {
            @Serializable
            class Driver(
                val x: Double,
                val y: Double,
                val heading: Angle,
            )
        }

        @Serializable
        @SerialName("Complete")
        class Complete(
            val results: Map<String, Result>
        ) : Event {
            @Serializable
            class Result(
                val place: Int,
                val time: Long,
            )
        }

    }
}
