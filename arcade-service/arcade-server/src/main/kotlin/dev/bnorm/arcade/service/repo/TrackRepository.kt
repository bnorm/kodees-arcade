package dev.bnorm.arcade.service.repo

import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.service.api.TrackId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object TrackTable : IdTable<TrackId>("tracks") {
    override val id: Column<EntityID<TrackId>> = trackId("id").clientDefault { TrackId.generate() }.entityId()
    val name = text("name").uniqueIndex()
    val width = double("width")
    val height = double("height")
    val checkpoints = jsonb<List<Segment>>("checkpoints", Json)
    val positions = jsonb<List<Position>>("positions", Json)

    override val primaryKey = PrimaryKey(id)
}

data class TrackEntity(
    val id: TrackId,
    val name: String,
    val width: Double,
    val height: Double,
    val checkpoints: List<Segment>,
    val positions: List<Position>,
)

fun ResultRow.toTrackEntity(): TrackEntity {
    return TrackEntity(
        id = this[TrackTable.id].value,
        name = this[TrackTable.name],
        width = this[TrackTable.width],
        height = this[TrackTable.height],
        checkpoints = this[TrackTable.checkpoints],
        positions = this[TrackTable.positions],
    )
}

@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class TrackRepository(
    private val database: R2dbcDatabase,
) : Repository {
    override suspend fun migrate() {
        suspendTransaction(database) {
            SchemaUtils.create(TrackTable)
        }
    }

    suspend fun getTracks(): List<TrackEntity> {
        return suspendTransaction(database) {
            TrackTable.selectAll().map { it.toTrackEntity() }.toList()
        }
    }

    suspend fun createTrack(
        name: String,
        width: Double,
        height: Double,
        checkpoints: List<Segment>,
        positions: List<Position>,
    ): TrackEntity {
        return suspendTransaction(database) {
            val id = TrackId.generate()
            TrackTable.insert {
                it[this.id] = id
                it[this.name] = name
                it[this.width] = width
                it[this.height] = height
                it[this.checkpoints] = checkpoints
                it[this.positions] = positions
            }

            TrackEntity(id, name, width, height, checkpoints, positions)
        }
    }

    suspend fun getTrack(id: TrackId): TrackEntity? {
        return suspendTransaction(database) {
            TrackTable.selectAll().where(TrackTable.id eq id)
                .singleOrNull()?.toTrackEntity()
        }
    }
}
