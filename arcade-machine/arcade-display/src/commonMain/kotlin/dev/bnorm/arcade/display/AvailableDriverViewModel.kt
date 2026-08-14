package dev.bnorm.arcade.display

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

interface AvailableDriverViewModel {
    fun watch(directory: PlatformFile)
    fun unwatch(key: String)

    val models: StateFlow<AvailableDriverModel>
}

class AvailableDriverModel(
    val watched: List<Watched>,
) {
    class Watched(
        val key: String,
        val directory: PlatformFile,
        val drivers: List<Driver>,
    ) {
        class Driver(
            val name: String,
            val file: PlatformFile,
        )
    }
}
