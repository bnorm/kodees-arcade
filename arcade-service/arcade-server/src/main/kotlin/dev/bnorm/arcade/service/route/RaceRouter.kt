package dev.bnorm.arcade.service.route

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.race.RaceService
import dev.bnorm.arcade.service.repo.RaceEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json

@ContributesIntoSet(AppScope::class)
class RaceRouter(
    private val races: RaceService,
) : Router {
    context(route: Route)
    override fun route() {
        route.route("/api/rally/races") {
            get {
                call.respond(races.getAllRaces())
            }

            post {
                val request = call.receive<RaceCreateRequest>()
                call.respond(races.createRace(request))
            }

            sse("/listen") {
                heartbeat()
                races.listen().collect {
                    send(Json.encodeToString(RaceProcessEvent.serializer(), it))
                }
            }

            get("/{raceId}") {
                val raceId = call.parameters.raceId
                val race = races.getRace(raceId) ?: throw NotFoundException()
                call.respond(race)
            }

            get("/{raceId}/download") {
                val raceId = call.parameters.raceId
                val (race, download) = races.downloadRace(raceId)
                when {
                    download != null -> {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment
                                .withParameter("filename", "${raceId}.race")
                                .toString()
                        )
                        call.respondBytesWriter { download.copyAndClose(this) }
                    }

                    race == null -> throw NotFoundException()
                    else -> call.respond(HttpStatusCode.NoContent)
                }
            }

            post("/{raceId}/upload") {
                val raceId = call.parameters.raceId
                val nonce = call.parameters.nonce

                val race =  races.uploadRace(raceId, nonce, call.receiveChannel())
                    ?: throw NotFoundException()

                call.respond(race)
            }
        }

        suspend fun process() {
            // TODO reset nonce and reprocess
        }
    }

    private fun RaceEntity.toResponse(): RaceResponse {
        return RaceResponse(
            id = this.id,
            trackId = this.trackId,
            startTime = this.startTime,
            endTime = this.endTime,
            racers = this.racers.toList(),
        )
    }

    private val Parameters.raceId: RaceId get() = RaceId(getUuid("raceId"))
    private val Parameters.nonce: Nonce get() = Nonce(getUuid("nonce"))
}
