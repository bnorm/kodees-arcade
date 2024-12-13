package dev.bnorm.arcade.runner

import dev.bnorm.arcade.agent.ArcadeAgent
import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.render.ArcadeRender
import java.net.URLClassLoader
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists

class JvmArcadeCartridge(
    private val classLoader: URLClassLoader,
    override val engineFactory: ArcadeEngine.Factory,
    override val renderFactory: ArcadeRender.Factory?,
) : ArcadeCartridge {
    override fun loadControllers(path: String): List<ArcadeController.Factory> {
        val file = Path(path)
        require(file.exists()) { "File does not exist: $path" }

        val uris = arrayOf(file.toUri().toURL())
        URLClassLoader.newInstance(uris, classLoader).use { classLoader ->
            val serviceLoader = ServiceLoader.load(ArcadeAgent::class.java, classLoader)
            return serviceLoader.filter { engineFactory.isSupported(it) }.map { agent ->
                val agentClass = agent::class.java
                JvmArcadeController.JvmFactory(
                    name = agentClass.simpleName,
                    factory = {
                        val controllerLoader = URLClassLoader.newInstance(uris, classLoader)
                        val klass = controllerLoader.loadClass(agentClass.name)
                        JvmArcadeController(
                            classLoader = controllerLoader,
                            agent = klass.getDeclaredConstructor().newInstance() as ArcadeAgent
                        )
                    }
                )
            }
        }
    }

    override fun close() {
        classLoader.close()
    }
}