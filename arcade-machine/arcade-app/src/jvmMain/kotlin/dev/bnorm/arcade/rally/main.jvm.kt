package dev.bnorm.arcade.rally

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.app.AppGraph
import dev.bnorm.arcade.display.InstallMenuItems
import dev.zacsweers.metro.createGraphFactory

fun main() {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.
    java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF) // Disable JUL.

    application {
        val scope = rememberCoroutineScope()
        val appGraph = remember(scope) {
            createGraphFactory<AppGraph.Factory>()
                .create(scope)
        }

        Window(
            title = "Rally",
            state = rememberWindowState(width = 1600.dp, height = 1000.dp),
            onCloseRequest = ::exitApplication,
        ) {
            val windowGraph = remember(appGraph) {
                appGraph.windowGraphFactory
                    .create(this@Window)
            }

            InstallMenuItems(windowGraph.items)
            appGraph.gameScreen.Content()
        }
    }
}
