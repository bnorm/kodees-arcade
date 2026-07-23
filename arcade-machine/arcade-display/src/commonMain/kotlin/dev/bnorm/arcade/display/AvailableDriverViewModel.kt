package dev.bnorm.arcade.display

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

interface AvailableDriverViewModel {
    fun watch(directory: PlatformFile)
    fun unwatch(watched: AvailableDriverModel.Watched)

    val models: StateFlow<AvailableDriverModel>
}

class AvailableDriverModel(
    val watched: List<Watched>,
    val drivers: List<Driver>,
) {
    class Watched(
        val key: String,
        val directory: PlatformFile,
    )

    class Driver(
        val name: String,
        val file: PlatformFile,
    )
}
