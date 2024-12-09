package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent

interface ArcadeController : AutoCloseable {
    val agent: ArcadeAgent
}
