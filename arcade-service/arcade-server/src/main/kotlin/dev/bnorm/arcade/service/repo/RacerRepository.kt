package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.Version
import io.ktor.utils.io.ByteReadChannel
import java.util.NavigableMap
import java.util.TreeMap
import kotlinx.coroutines.flow.groupBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
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

    override val primaryKey = PrimaryKey(id)
}

object RacerVersionTable : Table("racer_versions") {
    val racerId = reference("racer_id", RacerTable, onDelete = ReferenceOption.CASCADE)
    val version = version("version")
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(racerId, version)
}

data class RacerEntity(
    val id: RacerId,
    val name: String,
    val versions: NavigableMap<Version, BlobId>
)

fun ResultRow.toRacerEntity(versions: NavigableMap<Version, BlobId>): RacerEntity {
    return RacerEntity(
        id = this[RacerTable.id].value,
        name = this[RacerTable.name],
        versions = versions,
    )
}

class RacerRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository
) {
    suspend fun getRacers(): List<RacerEntity> {
        return suspendTransaction(database) {
            val racerVersions = RacerVersionTable.selectAll().groupBy(
                keySelector = { it[RacerVersionTable.racerId].value },
                valueTransform = { it[RacerVersionTable.version] to it[RacerVersionTable.blobId].value },
            )
            RacerTable.selectAll().map {
                val versions = buildNavigableMap {
                    for ((version, blobId) in racerVersions[it[RacerTable.id].value].orEmpty()) {
                        put(version, blobId)
                    }
                }
                it.toRacerEntity(versions)
            }.toList()
        }
    }

    suspend fun createRacer(name: String): RacerEntity {
        return suspendTransaction(database) {
            val id = RacerTable.insert {
                it[this.name] = name
            } get RacerTable.id

            RacerEntity(id.value, name, buildNavigableMap {})
        }
    }

    suspend fun uploadVersion(id: RacerId, version: Version, channel: ByteReadChannel): RacerEntity? {
        return suspendTransaction(database) {
            val racerRow = getRacerRow(id) ?: return@suspendTransaction null

            val blob = blobs.upload(channel)
            RacerVersionTable.insert {
                it[this.racerId] = id
                it[this.version] = version
                it[this.blobId] = blob.id
            }

            racerRow.toRacerEntity(getVersions(id))
        }
    }

    suspend fun getRacer(id: RacerId): RacerEntity? {
        return suspendTransaction(database) {
            getRacerRow(id)?.toRacerEntity(getVersions(id))
        }
    }

    private suspend fun getRacerRow(id: RacerId): ResultRow? {
        return RacerTable.selectAll().where(RacerTable.id eq id)
            .singleOrNull()
    }

    private suspend fun getVersions(id: RacerId): NavigableMap<Version, BlobId> {
        return buildNavigableMap {
            RacerVersionTable.selectAll().where(RacerVersionTable.racerId eq id)
                .collect { put(it[RacerVersionTable.version], it[RacerVersionTable.blobId].value) }
        }
    }
}

private inline fun <K, V> buildNavigableMap(block: MutableMap<K, V>.() -> Unit): NavigableMap<K, V> {
    return TreeMap<K, V>().apply(block)
}
