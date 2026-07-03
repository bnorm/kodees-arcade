package dev.bnorm.arcade.service.route

import dev.bnorm.arcade.service.api.DriverCreateRequest
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.DriverResponse
import dev.bnorm.arcade.service.api.DriverVersionResponse
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.BlobRepository
import dev.bnorm.arcade.service.repo.DriverEntity
import dev.bnorm.arcade.service.repo.DriverRepository
import dev.bnorm.arcade.service.repo.DriverVersionEntity
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
class DriverRouter(
    private val drivers: DriverRepository,
    private val blobs: BlobRepository,
) : Router {
    context(route: Route)
    override fun route() {
        route.route("/api/rally/drivers") {
            get {
                call.respond(drivers.getDrivers().map { it.toResponse() })
            }

            post {
                val request = call.receive<DriverCreateRequest>()
                call.respond(drivers.createDriver(request.name).toResponse())
            }

            get("/{driverId}") {
                val driverId = call.parameters.driverId
                val driver = drivers.getDriver(driverId) ?: throw NotFoundException()
                call.respond(driver.toResponse())
            }

            get("/{driverId}/versions") {
                val driverId = call.parameters.driverId
                val driver = drivers.getDriverVersions(driverId) // TODO throw NotFoundException()
                call.respond(driver.map { it.toResponse() })
            }

            get("/{driverId}/versions/{version}/download") {
                val driverId = call.parameters.driverId
                val version = call.parameters.version
                val driverEntity = drivers.getDriver(driverId) ?: throw NotFoundException()
                val versionEntity = drivers.getDriverVersion(driverId, version) ?: throw NotFoundException()
                val download = blobs.download(versionEntity.blobId) ?: error("should be impossible")

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter("filename", "${driverEntity.name}-${version}.wasm")
                        .toString()
                )
                call.respondBytesWriter { download.copyAndClose(this) }
            }

            post("/{driverId}/versions/{version}/upload") {
                val driverId = call.parameters.driverId
                val version = call.parameters.version

                val driver = drivers.uploadDriverVersion(driverId, version, call.receiveChannel())
                    ?: throw NotFoundException()

                call.respond(driver.toResponse())
            }
        }
    }

    private fun DriverEntity.toResponse(): DriverResponse {
        return DriverResponse(
            id = this.id,
            name = this.name,
        )
    }

    private fun DriverVersionEntity.toResponse(): DriverVersionResponse {
        return DriverVersionResponse(
            version = this.version,
        )
    }

    private val Parameters.driverId: DriverId get() = DriverId(getUuid("driverId"))
    private val Parameters.version: Version
        get() = Version.parse(
            this["version"] ?: throw MissingRequestParameterException("version")
        )
}
