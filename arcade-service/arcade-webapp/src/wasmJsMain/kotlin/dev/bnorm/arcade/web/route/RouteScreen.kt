package dev.bnorm.arcade.web.route

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.DefaultBinding
import kotlin.reflect.KClass

@DefaultBinding<RouteScreen<RouteKey>>
interface RouteScreen<K : RouteKey> {
    val key: KClass<out K>

    @Composable
    fun Content(key: K)
}
