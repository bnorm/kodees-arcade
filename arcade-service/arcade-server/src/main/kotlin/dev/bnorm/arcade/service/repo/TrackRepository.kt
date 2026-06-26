package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.TrackId
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object TrackTable : IdTable<TrackId>("tracks") {
    override val id: Column<EntityID<TrackId>> = trackId("id").clientDefault { TrackId.generate() }.entityId()
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(id)
}

data class TrackEntity(
    val id: TrackId,
    val blobId: BlobId,
)

fun ResultRow.toTrackEntity(): TrackEntity {
    return TrackEntity(
        id = this[TrackTable.id].value,
        blobId = this[TrackTable.blobId].value,
    )
}

class TrackRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository,
) {
    suspend fun getTracks(): List<TrackEntity> {
        return suspendTransaction(database) {
            TrackTable.selectAll().map { it.toTrackEntity() }.toList()
        }
    }

    suspend fun createTrack(json: String): TrackEntity {
        return suspendTransaction(database) {
            val blob = blobs.upload(json.byteInputStream().toByteReadChannel())
            val id = TrackTable.insert {
                it[blobId] = blob.id
            } get TrackTable.id
            getTrack(id.value)!!
        }
    }

    suspend fun getTrack(id: TrackId): TrackEntity? {
        return suspendTransaction(database) {
            TrackTable.selectAll().where(TrackTable.id eq id)
                .singleOrNull()?.toTrackEntity()
        }
    }
}
