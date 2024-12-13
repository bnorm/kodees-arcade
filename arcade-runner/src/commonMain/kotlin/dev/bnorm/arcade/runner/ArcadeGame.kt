package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.EngineState
import kotlinx.coroutines.channels.ReceiveChannel

interface ArcadeGame {
    val isRunning: Boolean
    val state: ReceiveChannel<EngineState>
}