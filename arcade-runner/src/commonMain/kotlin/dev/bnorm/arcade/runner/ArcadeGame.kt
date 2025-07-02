package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.engine.EngineResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

interface ArcadeGame {
    val state: StateFlow<EngineResult>
    suspend fun run()
}

fun ArcadeGame(
    cartridge: ArcadeCartridge,
    controllers: List<ArcadeController.Factory>
): ArcadeGame {
    val engine = cartridge.engineFactory.create(controllers.map { it.create().agent })
    val init = EngineResult.Running(engine.init())
    return ArcadeGameImpl(engine, init)
}

private class ArcadeGameImpl(
    private val engine: ArcadeEngine,
    init: EngineResult.Running,
) : ArcadeGame {
    private val _state = MutableStateFlow<EngineResult>(init)
    override val state: StateFlow<EngineResult>
        get() = _state.asStateFlow()

    override suspend fun run() {
        while (coroutineContext.isActive) {
            val result = engine.advance()
            _state.emit(result)
            if (result is EngineResult.Complete) break
        }
    }
}
