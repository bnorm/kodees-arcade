package dev.bnorm.arcade.cybertanks

import dev.bnorm.arcade.engine.EngineState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class CybertankEngineState(
    val tanks: List<Tank>
) : EngineState {

    @Serializable
    class Tank(
        val x: Double,
        val y: Double,
        val heading: Double,
        val gunHeading: Double,
        val radarHeading: Double,
    )

    override fun serialize(): ByteArray {
        return Json.encodeToString(serializer(), this).encodeToByteArray()
    }
}
