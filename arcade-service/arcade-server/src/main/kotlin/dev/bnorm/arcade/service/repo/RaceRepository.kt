package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.TrackId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.groupBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
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
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object RaceTable : IdTable<RaceId>("races") {
    override val id: Column<EntityID<RaceId>> = raceId("id").clientDefault { RaceId.generate() }.entityId()
    val trackId = reference("track_id", TrackTable)
    val blobId = reference("blob_id", BlobTable, onDelete = ReferenceOption.RESTRICT).nullable()

    override val primaryKey = PrimaryKey(id)
}

object RaceRacerTable : Table("races_racers") {
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.CASCADE)
    val racerId = reference("racer_id", RacerTable, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(raceId, racerId)
}

data class RaceEntity(
    val id: RaceId,
    val trackId: TrackId,
    val racers: List<RacerId>,
    val blobId: BlobId? = null,
)

fun ResultRow.toRaceEntity(racers: List<RacerId>): RaceEntity {
    return RaceEntity(
        id = this[RaceTable.id].value,
        trackId = this[RaceTable.trackId].value,
        racers = racers,
        blobId = this[RaceTable.blobId]?.value,
    )
}

@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class RaceRepository(
    private val database: R2dbcDatabase,
    private val blobs: BlobRepository,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(RaceTable, RaceRacerTable,)
        }
    }

    suspend fun getRaces(): List<RaceEntity> {
        return suspendTransaction(database) {
            val racerIds = RaceRacerTable.selectAll().groupBy(
                keySelector = { it[RaceRacerTable.raceId].value },
                valueTransform = { it[RaceRacerTable.racerId].value },
            )

            RaceTable.selectAll().map {
                val id = it[RaceTable.id].value
                it.toRaceEntity(racerIds[id].orEmpty())
            }.toList()
        }
    }

    suspend fun createRace(trackId: TrackId, racers: List<RacerId>): RaceEntity {
        return suspendTransaction(database) {
            val raceId = RaceTable.insert {
                it[this.trackId] = trackId
            } get RaceTable.id

            val racerIds = racers.map { racerId ->
                (RaceRacerTable.insert {
                    it[RaceRacerTable.raceId] = raceId
                    it[RaceRacerTable.racerId] = racerId
                } get RaceRacerTable.racerId).value
            }

            RaceTable.selectAll().where(RaceTable.id eq raceId)
                .single().toRaceEntity(racerIds)
        }
    }

    suspend fun finishRace(id: RaceId, channel: ByteReadChannel): RaceEntity? {
        return suspendTransaction(database) {
            val race = getRace(id) ?: return@suspendTransaction null
            val blob = blobs.upload(channel)
            RaceTable.update({ RaceTable.id eq id }) {
                it[RaceTable.blobId] = blob.id
            }
            race.copy(blobId = blob.id)
        }
    }

    suspend fun getRace(id: RaceId): RaceEntity? {
        return suspendTransaction(database) {
            val result =
                RaceTable.selectAll().where(RaceTable.id eq id).singleOrNull() ?: return@suspendTransaction null
            val racerIds = getRacerIds(id)
            result.toRaceEntity(racerIds)
        }
    }

    private suspend fun getRacerIds(id: RaceId): List<RacerId> {
        return RaceRacerTable.selectAll().where(RaceRacerTable.raceId eq id)
            .map { it[RaceRacerTable.racerId].value }.toList()
    }
}
