package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.BlobDirectory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.encoding.zstd.ZstdEncoder
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.nio.file.Files
import java.nio.file.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

@Serializable
@JvmInline
value class BlobId(val uuid: Uuid) {
    companion object {
        fun generate(): BlobId = BlobId(Uuid.generateV7())
    }
}

object BlobTable : IdTable<BlobId>("blobs") {
    override val id: Column<EntityID<BlobId>> = blobId("id").entityId()
    val path = nioPath("path")

    override val primaryKey = PrimaryKey(id)
}

data class BlobEntity(
    val id: BlobId,
    val path: Path,
)

fun ResultRow.toBlobEntity(): BlobEntity {
    return BlobEntity(
        id = this[BlobTable.id].value,
        path = this[BlobTable.path],
    )
}

@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class BlobRepository(
    private val database: R2dbcDatabase,
    @BlobDirectory private val directory: Path = Files.createTempDirectory("blobs"),
) : Repository {
    private val encoder = ZstdEncoder()

    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(BlobTable)
        }
    }

    suspend fun upload(channel: ByteReadChannel): BlobEntity {
        return suspendTransaction(database) {
            val id = BlobId.generate()
            val path = directory.resolve(id.uuid.toString())
            BlobTable.insert {
                it[this.id] = id
                it[this.path] = path
            }

            encoder.encode(channel).copyAndClose(path.toFile().writeChannel())

            BlobEntity(id, path)
        }
    }

    suspend fun download(id: BlobId): ByteReadChannel? {
        return suspendTransaction(database) {
            val blob = BlobTable.selectAll().where(BlobTable.id eq id)
                .singleOrNull()?.toBlobEntity()
            blob?.path?.readChannel()?.let { encoder.decode(it) }
        }
    }
}
