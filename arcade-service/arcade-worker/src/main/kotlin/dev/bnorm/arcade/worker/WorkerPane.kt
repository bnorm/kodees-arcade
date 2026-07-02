package dev.bnorm.arcade.worker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.StaticEffect
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.utils.io.ClosedWriteChannelException
import java.net.ConnectException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import dev.bnorm.arcade.machine.Race as RallyRace

sealed class ServerConnection {
    data object Connecting : ServerConnection()
    data object Connected : ServerConnection()
    class Failed(val error: Throwable) : ServerConnection()
}

@Composable
fun WorkerPane(client: ArcadeClient, jobs: Int) {
    val tracks = remember { mutableStateMapOf<TrackId, TrackResponse>() }
    suspend fun getTrack(id: TrackId): TrackResponse =
        tracks.getOrPut(id) { client.getTrack(id) }

    val racers = remember { mutableStateMapOf<RacerId, RacerResponse>() }
    suspend fun getRacer(id: RacerId): RacerResponse =
        racers.getOrPut(id) { client.getRacer(id) }

    val complete = remember { mutableStateListOf<RaceState>() }
    val active = remember { mutableStateListOf<RaceState>() }

    suspend fun process(events: ReceiveChannel<RaceProcessEvent>) {
        for (event in events) {
            val race = client.getRace(event.id)
            val state = RaceState(
                race = race,
                track = getTrack(race.trackId),
                racers = race.racers.map { getRacer(it) }
            )
            active += state

            val result = try {
                process(
                    client = client,
                    id = event.id,
                    nonce = event.nonce,
                    race = LocalRace(client, event.id),
                    state = state,
                ) ?: continue
            } finally {
                active -= state
            }
            state.race = result
            complete += state
        }
    }

    var connection by remember { mutableStateOf<ServerConnection>(ServerConnection.Connecting) }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                coroutineScope {
                    val events = client.listen().produceIn(this)
                    connection = ServerConnection.Connected
                    events.consume {
                        coroutineScope {
                            repeat(jobs) {
                                launch {
                                    process(events)
                                }
                            }
                        }
                    }
                }
                connection = ServerConnection.Connecting
            } catch (_: ConnectException) {
                connection = ServerConnection.Connecting
                delay(5.seconds)
            } catch (e: SSEClientException) {
                if (e.cause is ConnectException) {
                    connection = ServerConnection.Connecting
                    delay(5.seconds)
                } else {
                    connection = ServerConnection.Failed(e)
                    break
                }
            }
        }
    }

    for (state in complete) {
        StaticEffect {
            Column {
                Text(
                    value = buildString {
                        append(state.track.name)
                        append(" with ")
                        state.racers.joinTo(this, separator = ", ") { it.name }
                        append(" completed in ")
                        append(state.time)
                        append(" updates")
                    },
//                    modifier = Modifier
//                        .background(Color.Green)
                )
            }
        }
    }

    Column(Modifier.padding(left = 1, top = 1)) {
        when (val connection = connection) {
            ServerConnection.Connecting -> {
                Row {
                    BrailleSpinner()
                    Spacer(Modifier.width(1))
                    Text("Waiting for server connection...")
                }
            }

            is ServerConnection.Failed -> {
                Text(connection.error.stackTraceToString().replace("\t", "    "))
            }

            ServerConnection.Connected -> {
                for (state in active) {
                    Row {
                        BrailleSpinner()
                        Spacer(Modifier.width(1))
                        Text(
                            value = buildString {
                                append("Running ")
                                append(state.track.name)
                                append(" with ")
                                state.racers.joinTo(this, prefix = "[", postfix = "]") { it.name }

                                if (state.ups > 0) {
                                    append(" running at ")
                                    append(state.ups.toInt())
                                    append(" UPS")
                                }
                            },
//                    modifier = Modifier
//                        .background(Color.Yellow)
                        )
                    }
                }
                val idle = jobs - active.size
                if (idle > 0) {
                    Row {
                        BrailleSpinner()
                        Spacer(Modifier.width(1))
                        Text("$idle runners waiting for a race...")
                    }
                }
            }
        }
    }
}

class RaceState(
    race: RaceResponse,
    val track: TrackResponse,
    val racers: List<RacerResponse>,
) {
    var race by mutableStateOf(race)
    var time by mutableLongStateOf(0L)
    var ups by mutableDoubleStateOf(-1.0)
    var startTime by mutableStateOf<Instant?>(null)
    var endTime by mutableStateOf<Instant?>(null)
}

suspend fun process(
    client: ArcadeClient,
    id: RaceId,
    nonce: Nonce,
    race: RallyRace,
    state: RaceState,
): RaceResponse? {
    return coroutineScope {
        launch { race.start() }

        var targetTime = Instant.DISTANT_PAST
        val updateFrequency = 200.milliseconds

        val events = race.events
            .consumeAsFlow()
            .onEach {
                when (it) {
                    is RallyRace.Event.Start -> {
                        val now = Clock.System.now()
                        state.startTime = now
                        targetTime = now + 1.seconds
                    }

                    is RallyRace.Event.Update -> {
                        state.time = it.time
                        if (Clock.System.now() >= targetTime) {
                            val elapsed = Clock.System.now() - state.startTime!!
                            state.ups = state.time / (elapsed.inWholeNanoseconds / 1_000_000_000.0)
                            targetTime += updateFrequency
                        }
                    }

                    is RallyRace.Event.Complete -> {
                        val now = Clock.System.now()
                        state.endTime = now

                        val elapsed = now - state.startTime!!
                        state.ups = state.time / (elapsed.inWholeNanoseconds / 1_000_000_000.0)
                    }
                }
            }
            .map { ProtoBuf.encodeToByteArray(RallyRace.Event.serializer(), it) }
            .produceIn(this)

        try {
            client.upload(id, nonce, events)
        } catch (_: ClosedWriteChannelException) {
            // Ignore write closes, since we'll let the server handle retrying.
            null
        }
    }
}
