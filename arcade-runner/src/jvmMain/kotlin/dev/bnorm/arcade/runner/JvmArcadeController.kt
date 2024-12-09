package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent
import java.net.URLClassLoader

class JvmArcadeController(
    private val classLoader: URLClassLoader,
    override val agent: ArcadeAgent,
) : ArcadeController {
    override fun close() {
        classLoader.close()
    }
}