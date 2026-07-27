package dev.bnorm.arcade.service.track

import dev.bnorm.arcade.service.Router
import dev.bnorm.arcade.service.api.TrackCreateRequest
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.blob.BlobRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

@ContributesIntoSet(AppScope::class)
class TrackRouter(
    private val blobs: BlobRepository,
    private val tracks: TrackRepository,
) : Router {
    context(route: Route)
    override fun route() {
        route.route("/api/rally/tracks") {
            get {
                call.respond(tracks.getTracks().map { it.toResponse() })
            }

            post {
                val request = call.receive<TrackCreateRequest>()
                call.respond(
                    tracks.createTrack(
                        request.name,
                        request.width,
                        request.height,
                        request.checkpoints,
                        request.positions
                    ).toResponse()
                )
            }

            get("/{trackId}") {
                val trackId = call.parameters.trackId
                val track = tracks.getTrack(trackId) ?: throw NotFoundException()
                call.respond(track.toResponse())
            }
        }
    }

    private fun TrackEntity.toResponse(): TrackResponse {
        return TrackResponse(
            id = this.id,
            name = this.name,
            width = this.width,
            height = this.height,
            checkpoints = this.checkpoints,
            positions = this.positions,
        )
    }

    private val Parameters.trackId: TrackId get() = TrackId(getUuid("trackId"))
}
