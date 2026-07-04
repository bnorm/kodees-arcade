package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.repo.BlobId
import dev.bnorm.arcade.service.repo.BlobTable
import dev.bnorm.arcade.service.repo.DriverTable
import dev.bnorm.arcade.service.repo.DriverVersionId
import dev.bnorm.arcade.service.repo.DriverVersionTable
import dev.bnorm.arcade.service.repo.Repository
import dev.bnorm.arcade.service.repo.TrackTable
import dev.bnorm.arcade.service.repo.nonce
import dev.bnorm.arcade.service.repo.raceId
import dev.bnorm.arcade.service.season.SeasonRaceTable
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
    override val id: Column<EntityID<RaceId>> = raceId("id").entityId()
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

val RaceResultJoin = RaceTable.join(
    RaceResultTable,
    JoinType.LEFT,
    onColumn = RaceResultTable.raceId,
    otherColumn = RaceTable.id,
)

object PositionTable : Table("race_positions") {
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.CASCADE)
    val position = integer("position")
    val driverVersionId = reference("driver_version_id", DriverVersionTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(raceId, driverVersionId)
}

val PositionVersionDriverJoin = PositionTable
    .join(
        DriverVersionTable,
        JoinType.INNER,
        onColumn = DriverVersionTable.id,
        otherColumn = PositionTable.driverVersionId,
    )
    .join(
        DriverTable,
        JoinType.INNER,
        onColumn = DriverTable.id,
        otherColumn = DriverVersionTable.driverId,
    )

data class RaceEntity(
    val id: RaceId,
    val trackId: TrackId,
    val positions: List<PositionEntity>,
    val nonce: Nonce,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val blobId: BlobId? = null,
)

class PositionEntity(
    val position: Int,
    val driverId: DriverId,
    val driverVersionId: DriverVersionId,
    val name: String,
    val version: Version,
    val blobId: BlobId,
)

val SeasonRaceResultJoin = SeasonRaceTable
    .join(
        RaceTable,
        JoinType.INNER,
        onColumn = RaceTable.id,
        otherColumn = SeasonRaceTable.raceId,
    )
    .join(
        RaceResultTable,
        JoinType.LEFT,
        onColumn = RaceResultTable.raceId,
        otherColumn = RaceTable.id,
    )

val SeasonRaceDriverVersionJoin = SeasonRaceTable
    .join(
        PositionTable,
        JoinType.INNER,
        onColumn = PositionTable.raceId,
        otherColumn = SeasonRaceTable.raceId,
    )
    .join(
        DriverVersionTable,
        JoinType.INNER,
        onColumn = DriverVersionTable.id,
        otherColumn = PositionTable.driverVersionId,
    )
    .join(
        DriverTable,
        JoinType.INNER,
        onColumn = DriverTable.id,
        otherColumn = DriverVersionTable.driverId,
    )

fun ResultRow.toRaceEntity(
    positions: List<PositionEntity>,
): RaceEntity {
    return RaceEntity(
        id = this[RaceTable.id].value,
        trackId = this[RaceTable.trackId].value,
        positions = positions,
        nonce = this[RaceResultTable.nonce],
        startTime = this[RaceResultTable.startTime],
        endTime = this[RaceResultTable.endTime],
        blobId = this[RaceResultTable.blobId]?.value,
    )
}

fun ResultRow.toPositionEntity(): PositionEntity {
    return PositionEntity(
        position = this[PositionTable.position],
        driverId = this[DriverTable.id].value,
        driverVersionId = this[PositionTable.driverVersionId].value,
        name = this[DriverTable.name],
        version = this[DriverVersionTable.version],
        blobId = this[DriverVersionTable.blobId].value,
    )
}

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class RaceRepository(
    private val database: R2dbcDatabase,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(RaceTable, PositionTable, RaceResultTable)
        }
    }

    suspend fun getRaces(): List<RaceEntity> {
        // TODO paginate somehow
        return suspendTransaction(database) {
            val positionsByRaceId = PositionVersionDriverJoin.selectAll().groupBy(
                keySelector = { it[PositionTable.raceId].value },
                valueTransform = { it.toPositionEntity() },
            )

            // TODO array_agg?
            // RaceDriverTable.join(RaceResultsJoin, JoinType.RIGHT, RaceTable.id)
            //     .select(RaceDriverTable.driverId.function("ARRAY_AGG"), *RaceResultsJoin.columns.toTypedArray())

            RaceResultJoin.selectAll().map {
                val raceId = it[RaceTable.id].value
                it.toRaceEntity(positionsByRaceId[raceId].orEmpty())
            }.toList()
        }
    }

    suspend fun getRaces(seasonId: SeasonId): List<RaceEntity> {
        // TODO paginate somehow?
        return suspendTransaction(database) {
            val driverVersionIds = SeasonRaceDriverVersionJoin.selectAll()
                .where { SeasonRaceTable.seasonId eq seasonId }
                .groupBy(
                    keySelector = { it[PositionTable.raceId].value },
                    valueTransform = { it.toPositionEntity() },
                )

            SeasonRaceResultJoin.selectAll()
                .where { SeasonRaceTable.seasonId eq seasonId }
                .map {
                    val raceId = it[RaceTable.id].value
                    it.toRaceEntity(driverVersionIds[raceId].orEmpty())
                }
                .toList()
        }
    }

    /**
     * Specialized query to get only incomplete races.
     * Race entities are created without associated driver IDs to limit data.
     */
    suspend fun getIncompleteRaces(): List<RaceEntity> {
        // TODO paginate somehow?
        return suspendTransaction(database) {
            RaceResultJoin.selectAll()
                .where { RaceResultTable.endTime eq null }
                .map { it.toRaceEntity(emptyList()) }.toList()
        }
    }

    suspend fun createRace(seasonId: SeasonId?, trackId: TrackId, positions: List<DriverVersionId>): RaceEntity {
        return suspendTransaction(database) {
            val raceId = RaceId.generate()
            RaceTable.insert {
                it[this.id] = raceId
                it[this.trackId] = trackId
            }

            val nonce = Nonce.generate()
            RaceResultTable.insert {
                it[this.raceId] = raceId
                it[this.nonce] = nonce
            }

            PositionTable.batchInsert(positions.withIndex()) { (position, driverVersionId) ->
                this[PositionTable.raceId] = raceId
                this[PositionTable.position] = position
                this[PositionTable.driverVersionId] = driverVersionId
            }

            if (seasonId != null) {
                SeasonRaceTable.insert {
                    it[this.seasonId] = seasonId
                    it[this.raceId] = raceId
                }
            }

            RaceEntity(
                id = raceId,
                trackId = trackId,
                positions = getPositions(raceId),
                nonce = nonce,
            )
        }
    }

    suspend fun getRace(id: RaceId): RaceEntity? {
        return suspendTransaction(database) {
            val row = RaceResultJoin
                .selectAll()
                .where { RaceTable.id eq id }
                .singleOrNull()

            row ?: return@suspendTransaction null

            val versionedDrivers = getPositions(id)
            row.toRaceEntity(versionedDrivers)
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

    private suspend fun getPositions(id: RaceId): List<PositionEntity> {
        return PositionVersionDriverJoin.selectAll()
            .where { PositionTable.raceId eq id }
            .map { it.toPositionEntity() }
            .toList()
    }
}
