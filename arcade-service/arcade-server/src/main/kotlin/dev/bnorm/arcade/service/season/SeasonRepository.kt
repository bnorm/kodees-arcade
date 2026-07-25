package dev.bnorm.arcade.service.season

import dev.bnorm.arcade.service.DriverVersionId
import dev.bnorm.arcade.service.Repository
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.ParticipantId
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.Version
import dev.bnorm.arcade.service.driver.DriverTable
import dev.bnorm.arcade.service.driver.DriverVersionTable
import dev.bnorm.arcade.service.participantId
import dev.bnorm.arcade.service.race.RaceDriverTable
import dev.bnorm.arcade.service.race.RaceTable
import dev.bnorm.arcade.service.seasonId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object SeasonTable : IdTable<SeasonId>("seasons") {
    override val id = seasonId("id").entityId()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

class SeasonEntity(
    val id: SeasonId,
    val name: String,
)

fun ResultRow.toSeasonEntity(): SeasonEntity {
    return SeasonEntity(
        id = this[SeasonTable.id].value,
        name = this[SeasonTable.name],
    )
}

object ParticipantTable : IdTable<ParticipantId>("season_participants") {
    override val id = participantId("id").entityId()
    val seasonId = reference("season_id", SeasonTable, onDelete = ReferenceOption.CASCADE)
    val driverVersionId = reference("driver_version_id", DriverVersionTable, onDelete = ReferenceOption.RESTRICT)

    val score = double("score")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(seasonId, driverVersionId)
    }
}

val SeasonParticipantJoin = ParticipantTable
    .join(
        DriverVersionTable,
        JoinType.INNER,
        onColumn = DriverVersionTable.id,
        otherColumn = ParticipantTable.driverVersionId,
    )
    .join(
        DriverTable,
        JoinType.INNER,
        onColumn = DriverTable.id,
        otherColumn = DriverVersionTable.driverId,
    )

val RaceParticipantJoin = SeasonRaceTable
    .join(
        ParticipantTable,
        JoinType.INNER,
        onColumn = ParticipantTable.seasonId,
        otherColumn = SeasonRaceTable.seasonId
    )
    .join(
        RaceDriverTable,
        JoinType.INNER,
        onColumn = RaceDriverTable.driverVersionId,
        otherColumn = ParticipantTable.driverVersionId,
    )
    .join(
        DriverVersionTable,
        JoinType.INNER,
        onColumn = DriverVersionTable.id,
        otherColumn = ParticipantTable.driverVersionId,
    )
    .join(
        DriverTable,
        JoinType.INNER,
        onColumn = DriverTable.id,
        otherColumn = DriverVersionTable.driverId,
    )

class ParticipantEntity(
    val id: ParticipantId,
    val seasonId: SeasonId,
    val driverVersionId: DriverVersionId,
    val score: Double,
    val driverId: DriverId,
    val version: Version,
    val name: String,
)

fun ResultRow.toParticipantEntity(): ParticipantEntity {
    return ParticipantEntity(
        id = this[ParticipantTable.id].value,
        seasonId = this[ParticipantTable.seasonId].value,
        driverVersionId = this[ParticipantTable.driverVersionId].value,
        score = this[ParticipantTable.score],
        driverId = this[DriverVersionTable.driverId].value,
        version = this[DriverVersionTable.version],
        name = this[DriverTable.name],
    )
}

object SeasonRaceTable : Table("season_races") {
    val seasonId = reference("season_id", SeasonTable, onDelete = ReferenceOption.CASCADE)
    val raceId = reference("race_id", RaceTable, onDelete = ReferenceOption.RESTRICT).uniqueIndex()

    override val primaryKey = PrimaryKey(seasonId, raceId)
}

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class SeasonRepository(
    private val database: R2dbcDatabase,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(SeasonTable, ParticipantTable, SeasonRaceTable)
        }
    }

    suspend fun getSeasons(): List<SeasonEntity> {
        return suspendTransaction(database) {
            SeasonTable.selectAll()
                .map { it.toSeasonEntity() }
                .toList()
        }
    }

    suspend fun getSeason(id: SeasonId): SeasonEntity? {
        return suspendTransaction(database) {
            val row = SeasonTable.selectAll()
                .where { SeasonTable.id eq id }
                .singleOrNull()

            row?.toSeasonEntity()
        }
    }

    suspend fun createSeason(name: String): SeasonEntity {
        return suspendTransaction(database) {
            val id = SeasonId.generate()
            SeasonTable.insert {
                it[this.id] = id
                it[this.name] = name
            }

            SeasonEntity(id, name)
        }
    }

    suspend fun getParticipants(seasonId: SeasonId): List<ParticipantEntity> {
        return suspendTransaction(database) {
            SeasonParticipantJoin.selectAll()
                .where { ParticipantTable.seasonId eq seasonId }
                .map { it.toParticipantEntity() }
                .toList()
        }
    }

    suspend fun getParticipants(raceId: RaceId): List<ParticipantEntity> {
        return suspendTransaction(database) {
            RaceParticipantJoin.selectAll()
                .where { SeasonRaceTable.raceId eq raceId }
                .map { it.toParticipantEntity() }
                .toList()
        }
    }

    suspend fun getParticipant(seasonId: SeasonId, participantId: ParticipantId): ParticipantEntity? {
        return suspendTransaction(database) {
            val row = SeasonParticipantJoin.selectAll()
                .where {
                    (ParticipantTable.seasonId eq seasonId) and
                        (ParticipantTable.id eq participantId)
                }
                .singleOrNull()

            row?.toParticipantEntity()
        }
    }

    suspend fun createParticipant(seasonId: SeasonId, driverVersionId: DriverVersionId): ParticipantEntity {
        return suspendTransaction(database) {
            val participantId = ParticipantId.generate()
            ParticipantTable.insert {
                it[this.id] = participantId
                it[this.seasonId] = seasonId
                it[this.driverVersionId] = driverVersionId
                it[this.score] = 1200.0
            }

            getParticipant(seasonId, participantId)!!
        }
    }

    suspend fun deleteParticipant(seasonId: SeasonId, participantId: ParticipantId): Boolean {
        return suspendTransaction(database) {
            val rows = ParticipantTable.deleteWhere {
                (ParticipantTable.id eq participantId) and
                    (ParticipantTable.seasonId eq seasonId)
            }

            rows > 0
        }
    }

    suspend fun updateParticipant(seasonId: SeasonId, participantId: ParticipantId, score: Double): Boolean {
        return suspendTransaction(database) {
            val rows = ParticipantTable.update(
                where = {
                    (ParticipantTable.id eq participantId) and
                        (ParticipantTable.seasonId eq seasonId)
                }
            ) {
                it[this.score] = score
            }

            rows > 0
        }
    }
}
