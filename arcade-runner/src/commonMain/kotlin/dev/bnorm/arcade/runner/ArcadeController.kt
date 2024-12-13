package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent

interface ArcadeController : AutoCloseable {
    interface Factory {
        val name: String
        fun create(): ArcadeController
    }

    val agent: ArcadeAgent
}
