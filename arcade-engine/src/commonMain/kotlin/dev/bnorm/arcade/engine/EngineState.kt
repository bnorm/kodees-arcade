package dev.bnorm.arcade.engine

interface EngineState {
    fun serialize(): ByteArray
}