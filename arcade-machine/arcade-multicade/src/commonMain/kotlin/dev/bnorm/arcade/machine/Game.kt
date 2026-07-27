package dev.bnorm.arcade.machine

import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.geometry.Angle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface Game {
    suspend fun start(onEvent: suspend (Event) -> Unit)

    interface DriverDebug {
        fun isEnabled(driver: String): Boolean

        companion object {
            val Disabled = object : DriverDebug {
                override fun isEnabled(driver: String): Boolean = false
            }
        }
    }

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
                    val stdout: String?,
                    val stderr: String?,
                    val drawRequests: List<DrawRequest>,
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
                val laps: List<Long>,
            )
        }

    }
}
