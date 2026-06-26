package dev.bnorm.arcade.service

import dev.bnorm.arcade.rally.loadTrack
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.BlobTable
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.bnorm.arcade.service.repo.RaceRacerTable
import dev.bnorm.arcade.service.repo.RaceRepository
import dev.bnorm.arcade.service.repo.RaceTable
import dev.bnorm.arcade.service.repo.RacerEntity
import dev.bnorm.arcade.service.repo.RacerRepository
import dev.bnorm.arcade.service.repo.RacerTable
import dev.bnorm.arcade.service.repo.TrackEntity
import dev.bnorm.arcade.service.repo.TrackRepository
import dev.bnorm.arcade.service.repo.TrackTable
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.copyAndClose
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.toPath
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.bnorm.arcade.rally.Track as RallyTrack

fun main() {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

private fun Parameters.getUuid(name: String): Uuid {
    return Uuid.parse(this[name] ?: throw MissingRequestParameterException(name))
}

private val Parameters.raceId: RaceId get() = RaceId(getUuid("raceId"))
private val Parameters.racerId: RacerId get() = RacerId(getUuid("racerId"))
private val Parameters.trackId: TrackId get() = TrackId(getUuid("trackId"))

private fun RaceEntity.toResponse(): RaceResponse {
    return RaceResponse(
        id = this.id,
        trackId = this.trackId,
        racers = this.racers.toList(),
    )
}

private fun RacerEntity.toResponse(): RacerResponse {
    return RacerResponse(
        id = this.id,
        name = this.name,
    )
}

private fun TrackEntity.toResponse(): TrackResponse {
    return TrackResponse(
        id = this.id,
    )
}

private suspend fun Application.module() {
    val directory = Paths.get(".blobs")
    directory.deleteRecursively()
    directory.createDirectories()

    val database = R2dbcDatabase.connect(
        url = "r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1",
        databaseConfig = R2dbcDatabaseConfig {
            sqlLogger = Slf4jSqlDebugLogger
        }
    )

    suspendTransaction(database) {
        SchemaUtils.create(
            BlobTable,
            TrackTable,
            RaceTable,
            RacerTable,
            RaceRacerTable,
        )
    }

    val blobs = BlobRepository(database, directory)
    val racers = RacerRepository(database, blobs)
    val races = RaceRepository(database, blobs)
    val tracks = TrackRepository(database, blobs)
    val runner = RaceRunner(tracks, races, racers, blobs)
    val scope = CoroutineScope(Dispatchers.Default)

    tracks.addTrack("track.json")
    racers.addRacer("Kodee")
    racers.addRacer("Snail")

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate(4)
    }

    install(CallLogging) {
        callIdMdc("call-id")
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> call.respondText(
                    text = "400: ${cause.message}",
                    status = HttpStatusCode.BadRequest
                )

                else -> call.respondText(
                    text = "500: ${cause.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(SSE)

    routing {
        route("/api/rally") {
            route("/races") {
                get {
                    call.respond(races.getRaces().map { it.toResponse() })
                }

                post {
                    val request = call.receive<RaceCreateRequest>()
                    val race = races.createRace(request.trackId, request.racers)
                    scope.launch { runner.start(race.id) }
                    call.respond(race.toResponse())
                }

                route("/stream", HttpMethod.Post) {
                    sse {
                        val request = call.receive<RaceCreateRequest>()
                        val race = races.createRace(request.trackId, request.racers)

                        // TODO stream through blob instead?
                        //  - larger buffer
                        //  - doesn't delay completion of the race
                        val consumer = Channel<String>(1_000)
                        scope.launch { runner.start(race.id, consumer) }
                        send(Json.encodeToString(race.toResponse()))
                        consumer.consumeEach { send(it) }
                    }
                }

                get("/{raceId}") {
                    val raceId = call.parameters.raceId
                    val race = races.getRace(raceId) ?: throw NotFoundException()
                    call.respond(race.toResponse())
                }

                get("/{raceId}/download") {
                    val raceId = call.parameters.raceId
                    val race = races.getRace(raceId) ?: throw NotFoundException()
                    if (race.blobId != null) {
                        val download = blobs.download(race.blobId) ?: throw NotFoundException()

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter("filename", "${raceId.uuid}.race").toString()
                        )
                        call.respondBytesWriter { download.copyAndClose(this) }
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            route("/racers") {
                get {
                    call.respond(racers.getRacers().map { it.toResponse() })
                }
            }

            route("/tracks") {
                get {
                    call.respond(tracks.getTracks().map { it.toResponse() })
                }
            }
        }
    }
}

private suspend fun TrackRepository.addTrack(resource: String): TrackEntity {
    return createTrack(
        Json.encodeToString(
            RallyTrack.serializer(),
            loadTrack(ClassLoader.getSystemResource(resource).readText())
        )
    )
}

private suspend fun RacerRepository.addRacer(name: String): RacerEntity {
    return createRacer(
        name = name,
        channel = ClassLoader.getSystemResource("racers/files/$name.wasm")
            .toURI().toPath().readChannel()
    )
}
