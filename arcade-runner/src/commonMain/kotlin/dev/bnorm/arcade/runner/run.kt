package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.engine.EngineResult
import dev.bnorm.arcade.engine.EngineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun ArcadeEngine.run(): Flow<EngineState> = flow {
    while (true) {
        when (val result = advance()) {
            EngineResult.Complete -> break
            is EngineResult.Running -> {
                emit(result.state)
            }
        }
    }
}
