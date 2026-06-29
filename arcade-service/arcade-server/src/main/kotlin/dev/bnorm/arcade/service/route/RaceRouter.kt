package dev.bnorm.arcade.service.route

import dev.bnorm.arcade.service.RaceRunner
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.bnorm.arcade.service.repo.RaceRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.sse
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@ContributesIntoSet(AppScope::class)
class RaceRouter(
    private val races: RaceRepository,
    private val blobs: BlobRepository,
    private val runner: RaceRunner,
) : Router {
    // TODO inject
    private val scope = CoroutineScope(Dispatchers.Default)

    context(route: Route)
    override fun route() {
        route.route("/api/rally/races") {
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
                    val download = blobs.download(race.blobId) ?: error("should be impossible")

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment
                            .withParameter("filename", "${raceId.uuid}.race")
                            .toString()
                    )
                    call.respondBytesWriter { download.copyAndClose(this) }
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }

    private fun RaceEntity.toResponse(): RaceResponse {
        return RaceResponse(
            id = this.id,
            trackId = this.trackId,
            racers = this.racers.toList(),
        )
    }

    private val Parameters.raceId: RaceId get() = RaceId(getUuid("raceId"))
}
