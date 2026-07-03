package dev.bnorm.arcade.service

import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.loadTrack
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.DriverRepository
import dev.bnorm.arcade.service.repo.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import io.ktor.util.cio.readChannel
import kotlin.io.path.toPath
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class Initializer(
    private val tracks: TrackRepository,
    private val drivers: DriverRepository,
) : Service {
    override suspend fun initialize() {
        tracks.addTrack("track.json")
        drivers.addDriver("Kodee")
        drivers.addDriver("Snail")
    }

    private suspend fun TrackRepository.addTrack(resource: String) {
        createTrack(
            name = "Desk",
            json = Json.encodeToString(
                Track.serializer(),
                loadTrack(ClassLoader.getSystemResource(resource).readText())
            )
        )
    }

    private suspend fun DriverRepository.addDriver(name: String) {
        val driver = createDriver(name = name)
        uploadDriverVersion(
            driverId = driver.id,
            version = Version.parse("0.1.0"),
            channel = ClassLoader.getSystemResource("drivers/files/$name.wasm")
                .toURI().toPath().readChannel()
        )
    }
}
