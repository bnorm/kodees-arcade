package dev.bnorm.arcade.service.driver

import dev.bnorm.arcade.service.BlobId
import dev.bnorm.arcade.service.DriverVersionId
import dev.bnorm.arcade.service.Repository
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.blob.BlobRepository
import dev.bnorm.arcade.service.blob.BlobTable
import dev.bnorm.arcade.service.driverId
import dev.bnorm.arcade.service.driverVersionId
import dev.bnorm.arcade.service.version
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

object DriverTable : IdTable<DriverId>("drivers") {
    override val id: Column<EntityID<DriverId>> = driverId("id").clientDefault { DriverId.generate() }.entityId()
    val name = text("name").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object DriverVersionTable : IdTable<DriverVersionId>("driver_versions") {
    override val id = driverVersionId("id").clientDefault { DriverVersionId.generate() }.entityId()
    val driverId = reference("driver_id", DriverTable, onDelete = ReferenceOption.CASCADE)
    val version = version("version")
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(driverId, version)
    }
}

data class DriverEntity(
    val id: DriverId,
    val name: String,
)

fun ResultRow.toDriverEntity(): DriverEntity {
    return DriverEntity(
        id = this[DriverTable.id].value,
        name = this[DriverTable.name],
    )
}

class DriverVersionEntity(
    val id: DriverVersionId,
    val driverId: DriverId,
    val version: Version,
    val blobId: BlobId,
)

fun ResultRow.toDriverVersionEntity(): DriverVersionEntity {
    return DriverVersionEntity(
        id = this[DriverVersionTable.id].value,
        driverId = this[DriverVersionTable.driverId].value,
        version = this[DriverVersionTable.version],
        blobId = this[DriverVersionTable.blobId].value,
    )
}

@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DriverRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(DriverTable, DriverVersionTable)
        }
    }

    suspend fun getDrivers(): List<DriverEntity> {
        return suspendTransaction(database) {
            DriverTable.selectAll()
                .map { it.toDriverEntity() }
                .toList()
        }
    }

    suspend fun createDriver(name: String): DriverEntity {
        return suspendTransaction(database) {
            val id = DriverTable.insert {
                it[this.name] = name
            } get DriverTable.id

            DriverEntity(id.value, name)
        }
    }

    suspend fun getDriver(id: DriverId): DriverEntity? {
        return suspendTransaction(database) {
            getDriverRow(id)?.toDriverEntity()
        }
    }

    private suspend fun getDriverRow(id: DriverId): ResultRow? {
        return DriverTable.selectAll().where(DriverTable.id eq id)
            .singleOrNull()
    }

    suspend fun getDriverVersions(): List<DriverVersionEntity> {
        return suspendTransaction(database) {
            DriverVersionTable
                .selectAll()
                .map { it.toDriverVersionEntity() }
                .toList()
        }
    }

    suspend fun getDriverVersions(id: DriverId): List<DriverVersionEntity> {
        return suspendTransaction(database) {
            DriverVersionTable
                .selectAll()
                .where { DriverVersionTable.driverId eq id }
                .map { it.toDriverVersionEntity() }
                .toList()
        }
    }

    // TODO iterable of driver / version key pairs?
    suspend fun getDriverVersions(ids: Iterable<DriverId>): List<DriverVersionEntity> {
        return suspendTransaction(database) {
            DriverVersionTable
                .selectAll()
                .where { DriverVersionTable.driverId inList ids }
                .map { it.toDriverVersionEntity() }
                .toList()
        }
    }

    suspend fun getDriverVersion(id: DriverId, version: Version): DriverVersionEntity? {
        return suspendTransaction(database) {
            DriverVersionTable.selectAll()
                .where {
                    (DriverVersionTable.driverId eq id) and
                        (DriverVersionTable.version eq version)
                }
                .singleOrNull()
                ?.toDriverVersionEntity()
        }
    }

    suspend fun uploadDriverVersion(driverId: DriverId, version: Version, channel: ByteReadChannel): DriverVersionEntity? {
        return suspendTransaction(database) {
            val blob = blobs.upload(channel)

            // TODO insert first? to make sure version is unique for driver
            val id = DriverVersionTable.insert {
                it[this.driverId] = driverId
                it[this.version] = version
                it[this.blobId] = blob.id
            } get DriverVersionTable.id

            DriverVersionEntity(id.value, driverId, version, blob.id)
        }
    }
}
