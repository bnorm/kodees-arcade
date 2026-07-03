package dev.bnorm.arcade.service.route

import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.TrackEntity
import dev.bnorm.arcade.service.repo.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.utils.io.copyAndClose

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

            get("/{trackId}") {
                val trackId = call.parameters.trackId
                val track = tracks.getTrack(trackId) ?: throw NotFoundException()
                call.respond(track.toResponse())
            }

            get("/{trackId}/download") {
                val trackId = call.parameters.trackId
                val track = tracks.getTrack(trackId) ?: throw NotFoundException()
                val download = blobs.download(track.blobId) ?: error("should be impossible")

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter("filename", "${track.id}.json")
                        .toString()
                )
                call.respondBytesWriter { download.copyAndClose(this) }
            }
        }
    }

    private fun TrackEntity.toResponse(): TrackResponse {
        return TrackResponse(
            id = this.id,
            name = this.name,
        )
    }

    private val Parameters.trackId: TrackId get() = TrackId(getUuid("trackId"))
}
