package dev.bnorm.arcade.service

import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.loadTrack
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.RacerEntity
import dev.bnorm.arcade.service.repo.RacerRepository
import dev.bnorm.arcade.service.repo.TrackEntity
import dev.bnorm.arcade.service.repo.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.util.cio.readChannel
import kotlin.io.path.toPath
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class Initializer(
    private val tracks: TrackRepository,
    private val racers: RacerRepository,
) : Service {
    override suspend fun initialize() {
        tracks.addTrack("track.json")
        racers.addRacer("Kodee")
        racers.addRacer("Snail")
    }

    private suspend fun TrackRepository.addTrack(resource: String): TrackEntity {
        return createTrack(
            name = "Desk",
            json = Json.encodeToString(
                Track.serializer(),
                loadTrack(ClassLoader.getSystemResource(resource).readText())
            )
        )
    }

    private suspend fun RacerRepository.addRacer(name: String): RacerEntity {
        val racer = createRacer(name = name)
        return uploadVersion(
            id = racer.id,
            version = Version.parse("0.1.0"),
            channel = ClassLoader.getSystemResource("racers/files/$name.wasm")
                .toURI().toPath().readChannel()
        ) ?: racer
    }
}
