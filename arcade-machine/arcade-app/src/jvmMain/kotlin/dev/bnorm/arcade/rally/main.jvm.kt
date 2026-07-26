package dev.bnorm.arcade.rally

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.application
import dev.bnorm.arcade.app.AppGraph
import dev.zacsweers.metro.createGraphFactory

fun main() {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.
    java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF) // Disable JUL.

    // Set MacOS application name.
    // TODO do the same for windows and linux
    System.setProperty("apple.awt.application.name", "Kodee's Arcade")

    application {
        val scope = rememberCoroutineScope()
        val appGraph = remember(scope) {
            createGraphFactory<AppGraph.Factory>()
                .create(scope)
        }

        for (window in appGraph.windows) {
            with(window) {
                Content()
            }
        }
    }
}
