package dev.bnorm.arcade.cybertanks

import dev.bnorm.arcade.engine.EngineState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class CybertankEngineState(
    val tanks: List<Tank>,
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
        return ProtoBuf.encodeToByteArray(serializer(), this)
    }

    companion object {
        fun deserialize(data: ByteArray): CybertankEngineState {
            return ProtoBuf.decodeFromByteArray(serializer(), data)
        }
    }
}
