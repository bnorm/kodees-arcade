package dev.bnorm.arcade.worker

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import com.jakewharton.mosaic.runMosaic
import dev.bnorm.arcade.server.client.ArcadeClient
import okhttp3.OkHttpClient

class WorkerCommand : SuspendingCliktCommand() {
    val hostname: String by option().default("localhost")
    val port: Int? by option().int()
    val jobs: Int by option().int().default(4).validate { require(it > 0) }

    override suspend fun run() {
        val preconfigured = OkHttpClient()
        val client = ArcadeClient(hostname, port, preconfigured)

        runMosaic { WorkerPane(client, jobs) }
        // TODO non-interactive terminal version

        client.close()
        preconfigured.dispatcher.executorService.shutdown()
        preconfigured.connectionPool.evictAll()
        preconfigured.cache?.close()
    }
}
