package dev.bnorm.arcade.engine

import dev.bnorm.arcade.agent.ArcadeAgent

interface ArcadeEngine {
    interface Factory {
        fun isSupported(agent: ArcadeAgent): Boolean
        fun create(agents: List<ArcadeAgent>): ArcadeEngine
    }

    fun init(): EngineState
    fun advance(): EngineResult
}
