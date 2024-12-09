package dev.bnorm.arcade.runner

import dev.bnorm.arcade.engine.ArcadeEngine
import dev.bnorm.arcade.render.ArcadeRender
import java.net.URLClassLoader
import java.util.*
import kotlin.io.path.Path

actual fun loadCartridge(path: String): ArcadeCartridge {
    val url = Path(path).toUri().toURL()
    val classLoader = URLClassLoader.newInstance(arrayOf(url))
    val engineFactory = ServiceLoader.load(ArcadeEngine.Factory::class.java, classLoader).single()
    val renderFactory = ServiceLoader.load(ArcadeRender.Factory::class.java, classLoader).single()
    return JvmArcadeCartridge(
        classLoader = classLoader,
        engineFactory = engineFactory,
        renderFactory = renderFactory,
    )
}
