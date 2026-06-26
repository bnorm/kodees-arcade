package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.RacerId
import io.ktor.utils.io.ByteReadChannel
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

object RacerTable : IdTable<RacerId>("racers") {
    override val id: Column<EntityID<RacerId>> = racerId("id").clientDefault { RacerId.generate() }.entityId()
    val name = text("name")
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(id)
}

data class RacerEntity(
    val id: RacerId,
    val name: String,
    val blobId: BlobId,
)

fun ResultRow.toRacerEntity(): RacerEntity {
    return RacerEntity(
        id = this[RacerTable.id].value,
        name = this[RacerTable.name],
        blobId = this[RacerTable.blobId].value,
    )
}

class RacerRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository
) {
    suspend fun getRacers(): List<RacerEntity> {
        return suspendTransaction(database) {
            RacerTable.selectAll().map { it.toRacerEntity() }.toList()
        }
    }

    suspend fun createRacer(name: String, channel: ByteReadChannel): RacerEntity {
        return suspendTransaction(database) {
            val blob = blobs.upload(channel)
            val id = RacerTable.insert {
                it[this.name] = name
                it[this.blobId] = blob.id
            } get RacerTable.id
            getRacer(id.value)!!
        }
    }

    suspend fun getRacer(id: RacerId): RacerEntity? {
        return suspendTransaction(database) {
            RacerTable.selectAll().where(RacerTable.id eq id)
                .singleOrNull()?.toRacerEntity()
        }
    }
}
