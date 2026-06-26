package dev.bnorm.arcade.rally

import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class BlobId(val uuid: Uuid) {
    companion object {
        fun generate(): BlobId = BlobId(Uuid.generateV7())
    }
}

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
