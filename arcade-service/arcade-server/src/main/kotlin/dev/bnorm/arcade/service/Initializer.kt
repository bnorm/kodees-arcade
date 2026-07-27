package dev.bnorm.arcade.service

import dev.bnorm.arcade.service.driver.DriverRepository
import dev.bnorm.arcade.service.track.TrackRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class Initializer(
    private val tracks: TrackRepository,
    private val drivers: DriverRepository,
) : Service {
    override suspend fun initialize() {
//        tracks.addTrack("track.json")
//        drivers.addDriver("Kodee")
//        drivers.addDriver("Snail")
    }

//    private suspend fun TrackRepository.addTrack(resource: String) {
//        val track = loadTrack(ClassLoader.getSystemResource(resource).readText())
//        createTrack(
//            name = "Desk",
//            width = track.width,
//            height = track.height,
//            checkpoints = track.checkpoints,
//            positions = track.positions,
//        )
//    }
//
//    private suspend fun DriverRepository.addDriver(name: String) {
//        val driver = createDriver(name = name)
//        uploadDriverVersion(
//            driverId = driver.id,
//            version = Version.parse("0.1.0"),
//            channel = ClassLoader.getSystemResource("drivers/files/$name.wasm")
//                .toURI().toPath().readChannel()
//        )
//    }
}
