package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent
import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.render.ArcadeRender
import java.net.URLClassLoader
import java.util.*
import kotlin.io.path.Path

class JvmArcadeCartridge(
    private val classLoader: URLClassLoader,
    override val engineFactory: ArcadeEngine.Factory,
    override val renderFactory: ArcadeRender.Factory,
) : ArcadeCartridge {
    override fun loadControllers(path: String): List<JvmArcadeController> {
        val url = Path(path).toUri().toURL()
        URLClassLoader.newInstance(arrayOf(url), classLoader).use { classLoader ->
            val serviceLoader = ServiceLoader.load(ArcadeAgent::class.java, classLoader)
            return serviceLoader.map { agent ->
                val controllerLoader = URLClassLoader.newInstance(arrayOf(url), classLoader)
                val klass = controllerLoader.loadClass(agent::class.java.name)
                JvmArcadeController(controllerLoader, klass.getDeclaredConstructor().newInstance() as ArcadeAgent)
            }
        }
    }

    override fun close() {
        classLoader.close()
    }
}