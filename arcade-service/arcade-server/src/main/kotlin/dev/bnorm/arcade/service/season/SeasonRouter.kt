package dev.bnorm.arcade.service.season

import dev.bnorm.arcade.service.Router
import dev.bnorm.arcade.service.api.ParticipantCreateRequest
import dev.bnorm.arcade.service.api.ParticipantId
import dev.bnorm.arcade.service.api.SeasonCreateRequest
import dev.bnorm.arcade.service.api.SeasonId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class SeasonRouter(
    private val seasons: SeasonService,
) : Router {
    context(route: Route)
    override fun route() {
        route.route("/api/rally/seasons") {
            get {
                call.respond(seasons.getSeasons())
            }

            post {
                val request = call.receive<SeasonCreateRequest>()
                call.respond(seasons.createSeason(request))
            }

            get("/{seasonId}") {
                val seasonId = call.parameters.seasonId
                val season = seasons.getSeason(seasonId)
                    ?: throw NotFoundException()
                call.respond(season)
            }

            get("/{seasonId}/participants") {
                val seasonId = call.parameters.seasonId
                call.respond(seasons.getParticipants(seasonId))
            }

            post("/{seasonId}/participants") {
                val seasonId = call.parameters.seasonId
                val request = call.receive<ParticipantCreateRequest>()
                val participant = seasons.createParticipant(seasonId, request)
                    ?: throw NotFoundException()
                call.respond(participant)
            }

            get("/{seasonId}/participants/{participantId}") {
                val seasonId = call.parameters.seasonId
                val participantId = call.parameters.participantId
                val participant = seasons.getParticipant(seasonId, participantId)
                    ?: throw NotFoundException()
                call.respond(participant)
            }

            delete("/{seasonId}/participants/{participantId}") {
                val seasonId = call.parameters.seasonId
                val participantId = call.parameters.participantId
                val result = seasons.deleteParticipants(seasonId, participantId)
                call.respond(if (result) HttpStatusCode.Accepted else HttpStatusCode.NotFound)
            }
        }
    }

    private val Parameters.seasonId: SeasonId get() = SeasonId(getUuid("seasonId"))
    private val Parameters.participantId: ParticipantId get() = ParticipantId(getUuid("participantId"))
}
