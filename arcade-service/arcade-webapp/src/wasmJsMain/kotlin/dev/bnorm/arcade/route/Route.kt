package dev.bnorm.arcade.route

import androidx.compose.runtime.Composable

interface Route {
    val path: String

    @Composable
    fun Content()
}
