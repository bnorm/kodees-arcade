package dev.bnorm.arcade.worker

import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import dev.bnorm.arcade.machine.Race as RallyRace

suspend fun main() {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.

    // Worker:
    // 1. subscribe to server via SSE
    // 2. receive race and nonce
    // 3. stream race events to server together with nonce
    //     - should this happen *during* SSE event processing?
    //     - does this work to provide back-pressure?

    // Server:
    // 1. create a channel for sending races to workers
    // 2. reset nonce for all incomplete races and send to the channel
    // 3. each new created race is sent to the channel
    // 4. workers can subscribe to channel via SSE

    // If the worker disconnects during upload:
    //  - race nonce is reset
    //  - send race to channel for reprocessing

    // If the server disconnects:
    //  - cancel any current race and listen for more events

    // TODO parse url from args?
    //  - use clikt! https://ajalt.github.io/clikt/
    //  - use Mosaic?!?! https://github.com/jakewharton/mosaic
    val client = ArcadeClient()
    withContext(Dispatchers.IO) {
        while (true) {
            client.listen().collect {
                println("starting ${it.id}")
                process(client, it.id, it.nonce)
                println("finished ${it.id}")
            }
            // TODO make sure server disconnect is handled gracefully
        }
    }
}

suspend fun process(client: ArcadeClient, id: RaceId, nonce: Nonce): Unit = coroutineScope {
    val race = LocalRace(client, id)
    launch { race.start() }

    val events = race.events
        .consumeAsFlow()
        .map { Json.encodeToString(RallyRace.Event.serializer(), it) }
        .produceIn(this)

    client.upload(id, nonce, events)
}
