package dev.bnorm.arcade.web.route

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

interface RouteScreen<K : RouteKey> {
    val key: KClass<out K>

    @Composable
    fun Content(key: K)
}
