package dev.bnorm.arcade.main

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.bnorm.arcade.ui.ArcadeWindow
import org.jetbrains.compose.reload.DevelopmentEntryPoint

actual fun run() {
    application {
        Window(
            title = "Kodee's Arcade",
            onCloseRequest = ::exitApplication,
            alwaysOnTop = true, // TODO temporary to help development workflow.
        ) {
            DevelopmentEntryPoint {
                ArcadeWindow()
            }
        }
    }
}
