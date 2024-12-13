package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.render.ArcadeRender

interface ArcadeCartridge : AutoCloseable {
    val engineFactory: ArcadeEngine.Factory
    val renderFactory: ArcadeRender.Factory?

    fun loadControllers(path: String): List<ArcadeController.Factory>
}
