package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.Version
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object RacerTable : IdTable<RacerId>("racers") {
    override val id: Column<EntityID<RacerId>> = racerId("id").clientDefault { RacerId.generate() }.entityId()
    val name = text("name").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object RacerVersionTable : IdTable<RacerVersionId>("racer_versions") {
    override val id = racerVersionId("id").clientDefault { RacerVersionId.generate() }.entityId()
    val racerId = reference("racer_id", RacerTable, onDelete = ReferenceOption.CASCADE)
    val version = version("version")
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(racerId, version)
    }
}

data class RacerEntity(
    val id: RacerId,
    val name: String,
)

fun ResultRow.toRacerEntity(): RacerEntity {
    return RacerEntity(
        id = this[RacerTable.id].value,
        name = this[RacerTable.name],
    )
}

class RacerVersionEntity(
    val id: RacerVersionId,
    val racerId: RacerId,
    val version: Version,
    val blobId: BlobId,
)

fun ResultRow.toRacerVersionEntity(): RacerVersionEntity {
    return RacerVersionEntity(
        id = this[RacerVersionTable.id].value,
        racerId = this[RacerVersionTable.racerId].value,
        version = this[RacerVersionTable.version],
        blobId = this[RacerVersionTable.blobId].value,
    )
}

@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RacerRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(RacerTable, RacerVersionTable)
        }
    }

    suspend fun getRacers(): List<RacerEntity> {
        return suspendTransaction(database) {
            RacerTable.selectAll()
                .map { it.toRacerEntity() }
                .toList()
        }
    }

    suspend fun createRacer(name: String): RacerEntity {
        return suspendTransaction(database) {
            val id = RacerTable.insert {
                it[this.name] = name
            } get RacerTable.id

            RacerEntity(id.value, name)
        }
    }

    suspend fun getRacer(id: RacerId): RacerEntity? {
        return suspendTransaction(database) {
            getRacerRow(id)?.toRacerEntity()
        }
    }

    private suspend fun getRacerRow(id: RacerId): ResultRow? {
        return RacerTable.selectAll().where(RacerTable.id eq id)
            .singleOrNull()
    }

    suspend fun getRacerVersions(): List<RacerVersionEntity> {
        return suspendTransaction(database) {
            RacerVersionTable
                .selectAll()
                .map { it.toRacerVersionEntity() }
                .toList()
        }
    }

    suspend fun getRacerVersions(id: RacerId): List<RacerVersionEntity> {
        return suspendTransaction(database) {
            RacerVersionTable
                .selectAll()
                .where { RacerVersionTable.racerId eq id }
                .map { it.toRacerVersionEntity() }
                .toList()
        }
    }

    // TODO iterable of racer / version key pairs?
    suspend fun getRacerVersions(ids: Iterable<RacerId>): List<RacerVersionEntity> {
        return suspendTransaction(database) {
            RacerVersionTable
                .selectAll()
                .where { RacerVersionTable.racerId inList ids }
                .map { it.toRacerVersionEntity() }
                .toList()
        }
    }

    suspend fun getRacerVersion(id: RacerId, version: Version): RacerVersionEntity? {
        return suspendTransaction(database) {
            RacerVersionTable.selectAll()
                .where {
                    (RacerVersionTable.racerId eq id) and
                        (RacerVersionTable.version eq version)
                }
                .singleOrNull()
                ?.toRacerVersionEntity()
        }
    }

    suspend fun uploadRacerVersion(racerId: RacerId, version: Version, channel: ByteReadChannel): RacerVersionEntity? {
        return suspendTransaction(database) {
            val blob = blobs.upload(channel)
            val id = RacerVersionTable.insert {
                it[this.racerId] = racerId
                it[this.version] = version
                it[this.blobId] = blob.id
            } get RacerVersionTable.id

            RacerVersionEntity(id.value, racerId, version, blob.id)
        }
    }
}
