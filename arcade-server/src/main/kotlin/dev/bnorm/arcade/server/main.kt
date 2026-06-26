package dev.bnorm.arcade.server

import dev.bnorm.arcade.rally.BlobRepository
import dev.bnorm.arcade.rally.loadTrack
import dev.bnorm.arcade.server.rally.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.toPath
import kotlin.uuid.Uuid
import dev.bnorm.arcade.rally.Track as RallyTrack

fun main() {
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

@Serializable
class RaceCreateRequest(
    val trackId: TrackId,
    val racers: List<RacerId>,
)

private suspend fun Application.module() {
    val directory = Paths.get(".blobs")
    directory.deleteRecursively()
    directory.createDirectories()
    val blobs = BlobRepository(directory)

    val racers = RacerRepository(blobs)
    val races = RaceRepository(blobs)
    val tracks = TrackRepository(blobs)
    val runner = RaceRunner(tracks, races, racers, blobs)
    val scope = CoroutineScope(Dispatchers.Default)

    val trackId = tracks.createTrack(
        Json.encodeToString(
            RallyTrack.serializer(),
            loadTrack(ClassLoader.getSystemResource("track.json").readText())
        )
    )
    val kodeeId = racers.addRacer("Kodee")
    val snailId = racers.addRacer("Snail")

    println("trackId = $trackId")
    println("kodeeId = $kodeeId")
    println("snailId = $snailId")

    println(
        """
        {
          "trackId": "${trackId.uuid}",
          "racers": [
            "${snailId.uuid}",
            "${kodeeId.uuid}"
          ]
        }
        """.trimIndent()
    )

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

    routing {
        route("/api/rally") {
            route("/race") {
                get {
                    call.respond(races.getRaces())
                }

                post {
                    val request = call.receive<RaceCreateRequest>()
                    val race = races.createRace(request.trackId, request.racers)
                    scope.launch { runner.start(race.id) }
                    call.respond(race)
                }


                get("/{raceId}") {
                    val raceId = call.parameters.raceId
                    val race = races.getRace(raceId) ?: throw NotFoundException()
                    call.respond(race)
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
        }
    }
}

private suspend fun RacerRepository.addRacer(name: String): RacerId {
    return createRacer(name, ClassLoader.getSystemResource("racers/files/$name.wasm").toURI().toPath().readChannel())
}
