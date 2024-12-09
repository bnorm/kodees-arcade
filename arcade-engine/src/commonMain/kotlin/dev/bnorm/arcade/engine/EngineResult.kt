package dev.bnorm.arcade.engine

sealed class EngineResult {
    data object Complete : EngineResult()
    class Running(val state: EngineState) : EngineResult()
}