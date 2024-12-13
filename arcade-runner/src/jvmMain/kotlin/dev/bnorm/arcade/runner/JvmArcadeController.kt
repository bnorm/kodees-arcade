package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent
import java.net.URLClassLoader

class JvmArcadeController(
    private val classLoader: URLClassLoader,
    override val agent: ArcadeAgent,
) : ArcadeController {
    class JvmFactory(
        override val name: String,
        private val factory: () -> ArcadeController,
    ) : ArcadeController.Factory {
        override fun create(): ArcadeController {
            return factory()
        }
    }

    override fun close() {
        classLoader.close()
    }
}