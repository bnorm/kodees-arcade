package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.BlobId
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.nio.file.Files
import java.nio.file.Path

class BlobRepository(
    private val directory: Path = Files.createTempDirectory("blobs"),
) {
    private val blobs = mutableMapOf<BlobId, Path>()

    suspend fun upload(channel: ByteReadChannel): BlobId {
        val id = BlobId.generate()
        val path = directory.resolve(id.uuid.toString())
        channel.copyAndClose(path.toFile().writeChannel())

        blobs[id] = path
        return id
    }

    fun download(id: BlobId): ByteReadChannel? {
        return blobs[id]?.readChannel()
    }
}
