package dev.bnorm.arcade.worker

import com.github.ajalt.clikt.command.main

suspend fun main(args: Array<String>) {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.
    java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF) // Disable JUL.
    WorkerCommand().main(args)
}
