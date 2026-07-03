package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.BlobId
import dev.bnorm.arcade.service.repo.BlobTable
import dev.bnorm.arcade.service.repo.RacerTable
import dev.bnorm.arcade.service.repo.RacerVersionId
import dev.bnorm.arcade.service.repo.RacerVersionTable
import dev.bnorm.arcade.service.repo.Repository
import dev.bnorm.arcade.service.repo.TrackTable
import dev.bnorm.arcade.service.repo.nonce
import dev.bnorm.arcade.service.repo.raceId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlin.time.Instant
import kotlinx.coroutines.flow.groupBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object RaceTable : IdTable<RaceId>("races") {
    override val id: Column<EntityID<RaceId>> = raceId("id").clientDefault { RaceId.generate() }.entityId()
    val trackId = reference("track_id", TrackTable)

    override val primaryKey = PrimaryKey(id)
}

// TODO does this really need to be another table?
object RaceResultTable : Table("race_results") {
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.CASCADE)
    val nonce = nonce("nonce")

    val startTime = timestamp("start_time").nullable()

    val endTime = timestamp("end_time").nullable()
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT).nullable()

    override val primaryKey = PrimaryKey(raceId)
}

val RaceResultsJoin = RaceTable.join(
    RaceResultTable,
    JoinType.LEFT,
)

object RaceRacerTable : Table("race_racers") {
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.CASCADE)
    val racerVersionId = reference("racer_version_id", RacerVersionTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(raceId, racerVersionId)
}

val RaceRacerVersionJoin = RaceRacerTable
    .join(
        RacerVersionTable,
        JoinType.INNER,
    )
    .join(
        RacerTable,
        JoinType.INNER,
        onColumn = RacerTable.id,
        otherColumn = RacerVersionTable.racerId,
    )

data class RaceEntity(
    val id: RaceId,
    val trackId: TrackId,
    val versionedRacers: List<VersionedRacerEntity>,
    val nonce: Nonce,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val blobId: BlobId? = null,
)

class VersionedRacerEntity(
    val racerId: RacerId,
    val racerVersionId: RacerVersionId,
    val name: String,
    val version: Version,
    val blobId: BlobId,
)

fun ResultRow.toRaceEntity(
    versionedRacers: List<VersionedRacerEntity>,
): RaceEntity {
    return RaceEntity(
        id = this[RaceTable.id].value,
        trackId = this[RaceTable.trackId].value,
        versionedRacers = versionedRacers,
        nonce = this[RaceResultTable.nonce],
        startTime = this[RaceResultTable.startTime],
        endTime = this[RaceResultTable.endTime],
        blobId = this[RaceResultTable.blobId]?.value,
    )
}

fun ResultRow.toVersionedRacerEntity(): VersionedRacerEntity {
    return VersionedRacerEntity(
        racerId = this[RacerTable.id].value,
        racerVersionId = this[RacerVersionTable.id].value,
        name = this[RacerTable.name],
        version = this[RacerVersionTable.version],
        blobId = this[RacerVersionTable.blobId].value,
    )
}

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class RaceRepository(
    private val database: R2dbcDatabase,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(RaceTable, RaceRacerTable, RaceResultTable)
        }
    }

    suspend fun getRaces(): List<RaceEntity> {
        // TODO paginate somehow
        return suspendTransaction(database) {
            val racerVersionIds = RaceRacerVersionJoin.selectAll().groupBy(
                keySelector = { it[RaceRacerTable.raceId].value },
                valueTransform = { it.toVersionedRacerEntity() },
            )

            // TODO array_agg?
            // RaceRacerTable.join(RaceResultsJoin, JoinType.RIGHT, RaceTable.id)
            //     .select(RaceRacerTable.racerId.function("ARRAY_AGG"), *RaceResultsJoin.columns.toTypedArray())

            RaceResultsJoin.selectAll().map {
                val id = it[RaceTable.id].value
                it.toRaceEntity(racerVersionIds[id].orEmpty())
            }.toList()
        }
    }

    /**
     * Specialized query to get only incomplete races.
     * Race entities are created without associated racer IDs to limit data.
     */
    suspend fun getIncompleteRaces(): List<RaceEntity> {
        // TODO paginate somehow?
        return suspendTransaction(database) {
            RaceResultsJoin.selectAll()
                .where { RaceResultTable.endTime eq null }
                .map { it.toRaceEntity(emptyList()) }.toList()
        }
    }

    suspend fun createRace(trackId: TrackId, racerVersionIds: List<RacerVersionId>): RaceEntity {
        return suspendTransaction(database) {
            val raceId = (RaceTable.insert {
                it[this.trackId] = trackId
            } get RaceTable.id).value

            val nonce = Nonce.generate()
            RaceResultTable.insert {
                it[this.raceId] = raceId
                it[this.nonce] = nonce
            }

            RaceRacerTable.batchInsert(racerVersionIds) {
                this[RaceRacerTable.raceId] = raceId
                this[RaceRacerTable.racerVersionId] = it
            }

            RaceEntity(
                id = raceId,
                trackId = trackId,
                versionedRacers = getVersionedRacers(raceId),
                nonce = nonce,
            )
        }
    }

    suspend fun getRace(id: RaceId): RaceEntity? {
        return suspendTransaction(database) {
            val row = RaceResultsJoin
                .selectAll()
                .where { RaceTable.id eq id }
                .singleOrNull()

            row ?: return@suspendTransaction null

            val versionedRacers = getVersionedRacers(id)
            row.toRaceEntity(versionedRacers)
        }
    }

    suspend fun startRace(id: RaceId, nonce: Nonce, startTime: Instant): Boolean {
        return suspendTransaction(database) {
            RaceResultTable.update(
                where = {
                    (RaceResultTable.raceId eq id) and
                        (RaceResultTable.nonce eq nonce)
                }
            ) {
                it[RaceResultTable.startTime] = startTime
            } == 1
        }
    }

    suspend fun finishRace(id: RaceId, nonce: Nonce, endTime: Instant, blobId: BlobId): Boolean {
        return suspendTransaction(database) {
            RaceResultTable.update(
                where = {
                    (RaceResultTable.raceId eq id) and
                        (RaceResultTable.nonce eq nonce)
                }
            ) {
                it[RaceResultTable.endTime] = endTime
                it[RaceResultTable.blobId] = blobId
            } == 1
        }
    }

    suspend fun resetRace(id: RaceId): RaceEntity? {
        return suspendTransaction(database) {
            val rows = RaceResultTable.update(
                where = {
                    (RaceResultTable.raceId eq id) and
                        (RaceResultTable.blobId eq null)
                }
            ) {
                it[RaceResultTable.nonce] = Nonce.generate()
                it[RaceResultTable.startTime] = null
            }
            if (rows != 1) return@suspendTransaction null

            getRace(id)
        }
    }

    private suspend fun getVersionedRacers(id: RaceId): List<VersionedRacerEntity> {
        return RaceRacerVersionJoin.selectAll()
            .where { RaceRacerTable.raceId eq id }
            .map { it.toVersionedRacerEntity() }
            .toList()
    }
}
