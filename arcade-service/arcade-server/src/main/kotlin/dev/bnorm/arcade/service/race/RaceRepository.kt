package dev.bnorm.arcade.service.race

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.repo.BlobId
import dev.bnorm.arcade.service.repo.BlobTable
import dev.bnorm.arcade.service.repo.RacerTable
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
import kotlinx.coroutines.flow.single
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

object RaceRacerTable : Table("race_racers") {
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.CASCADE)
    val racerId = reference("racer_id", RacerTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(raceId, racerId)
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

data class RaceEntity(
    val id: RaceId,
    val trackId: TrackId,
    val racers: List<RacerId>,
    val nonce: Nonce,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val blobId: BlobId? = null,
)

fun ResultRow.toRaceEntity(
    racers: List<RacerId>,
): RaceEntity {
    return RaceEntity(
        id = this[RaceTable.id].value,
        trackId = this[RaceTable.trackId].value,
        racers = racers,
        nonce = this[RaceResultTable.nonce],
        startTime = this[RaceResultTable.startTime],
        endTime = this[RaceResultTable.endTime],
        blobId = this[RaceResultTable.blobId]?.value,
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
            val racerIds = RaceRacerTable.selectAll().groupBy(
                keySelector = { it[RaceRacerTable.raceId].value },
                valueTransform = { it[RaceRacerTable.racerId].value },
            )

            // TODO array_agg?
            // RaceRacerTable.join(RaceResultsJoin, JoinType.RIGHT, RaceTable.id)
            //     .select(RaceRacerTable.racerId.function("ARRAY_AGG"), *RaceResultsJoin.columns.toTypedArray())

            RaceResultsJoin.selectAll().map {
                val id = it[RaceTable.id].value
                it.toRaceEntity(racerIds[id].orEmpty())
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

    suspend fun createRace(trackId: TrackId, racers: List<RacerId>): RaceEntity {
        return suspendTransaction(database) {
            val raceId = RaceTable.insert {
                it[this.trackId] = trackId
            } get RaceTable.id

            RaceResultTable.insert {
                it[this.raceId] = raceId.value
                it[this.nonce] = Nonce.generate()
            }

            val racerIds = RaceRacerTable.batchInsert(racers) {
                this[RaceRacerTable.raceId] = raceId
                this[RaceRacerTable.racerId] = it
            }.map { it[RaceRacerTable.racerId].value }

            RaceResultsJoin.selectAll().where(RaceTable.id eq raceId)
                .single().toRaceEntity(racerIds)
        }
    }

    suspend fun getRace(id: RaceId): RaceEntity? {
        return suspendTransaction(database) {
            val row = RaceResultsJoin
                .selectAll()
                .where { RaceTable.id eq id }
                .singleOrNull()

            row ?: return@suspendTransaction null

            val racerIds = getRacerIds(id)
            row.toRaceEntity(racerIds)
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

    private suspend fun getRacerIds(id: RaceId): List<RacerId> {
        return RaceRacerTable.selectAll().where(RaceRacerTable.raceId eq id)
            .map { it[RaceRacerTable.racerId].value }.toList()
    }
}
