package dev.bnorm.arcade.machine

import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.rally.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface Game {
    suspend fun start(onEvent: suspend (Event) -> Unit)
    fun setDebug(driver: String, debug: Boolean) {}

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
                @Transient val debug: Debug? = null,
            ) {
                class Debug(
                    val stdout: List<String>,
                    val canvasRequests: List<DrawRequest>,
                )

            }
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
