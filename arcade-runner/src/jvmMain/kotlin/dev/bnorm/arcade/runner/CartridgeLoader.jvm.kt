package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.render.ArcadeRender
import java.net.URLClassLoader
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists

actual fun loadCartridge(path: String): ArcadeCartridge? {
    val file = Path(path)
    require(file.exists()) { "File does not exist: $path" }

    val classLoader = URLClassLoader.newInstance(arrayOf(file.toUri().toURL()))
    val engineFactories = ServiceLoader.load(ArcadeEngine.Factory::class.java, classLoader).toList()
    val renderFactory = ServiceLoader.load(ArcadeRender.Factory::class.java, classLoader).toList()
    require(engineFactories.size < 2 && renderFactory.size < 2) { "Multiple ArcadeEngines or ArcadeRenders found" }

    return JvmArcadeCartridge(
        classLoader = classLoader,
        engineFactory = engineFactories.firstOrNull() ?: return null,
        renderFactory = renderFactory.firstOrNull(),
    )
}
