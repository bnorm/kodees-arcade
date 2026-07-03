package dev.bnorm.arcade.service.route

import dev.bnorm.arcade.service.api.RacerCreateRequest
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.RacerVersionResponse
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.RacerEntity
import dev.bnorm.arcade.service.repo.RacerRepository
import dev.bnorm.arcade.service.repo.RacerVersionEntity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.server.plugins.MissingRequestParameterException
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
import io.ktor.utils.io.copyAndClose

@ContributesIntoSet(AppScope::class)
class RacerRouter(
    private val racers: RacerRepository,
    private val blobs: BlobRepository,
) : Router {
    context(route: Route)
    override fun route() {
        route.route("/api/rally/racers") {
            get {
                call.respond(racers.getRacers().map { it.toResponse() })
            }

            post {
                val request = call.receive<RacerCreateRequest>()
                call.respond(racers.createRacer(request.name).toResponse())
            }

            get("/{racerId}") {
                val racerId = call.parameters.racerId
                val racer = racers.getRacer(racerId) ?: throw NotFoundException()
                call.respond(racer.toResponse())
            }

            get("/{racerId}/versions") {
                val racerId = call.parameters.racerId
                val racer = racers.getRacerVersions(racerId) // TODO throw NotFoundException()
                call.respond(racer.map { it.toResponse() })
            }

            get("/{racerId}/download/{version}") {
                val racerId = call.parameters.racerId
                val version = call.parameters.version
                val racerEntity = racers.getRacer(racerId) ?: throw NotFoundException()
                val versionEntity = racers.getRacerVersion(racerId, version) ?: throw NotFoundException()
                val download = blobs.download(versionEntity.blobId) ?: error("should be impossible")

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter("filename", "${racerEntity.name}-${version}.wasm")
                        .toString()
                )
                call.respondBytesWriter { download.copyAndClose(this) }
            }

            post("/{racerId}/upload/{version}") {
                val racerId = call.parameters.racerId
                val version = call.parameters.version

                val racer = racers.uploadRacerVersion(racerId, version, call.receiveChannel())
                    ?: throw NotFoundException()

                call.respond(racer.toResponse())
            }
        }
    }

    private fun RacerEntity.toResponse(): RacerResponse {
        return RacerResponse(
            id = this.id,
            name = this.name,
        )
    }

    private fun RacerVersionEntity.toResponse(): RacerVersionResponse {
        return RacerVersionResponse(
            version = this.version,
        )
    }

    private val Parameters.racerId: RacerId get() = RacerId(getUuid("racerId"))
    private val Parameters.version: Version
        get() = Version.parse(
            this["version"] ?: throw MissingRequestParameterException("version")
        )
}
