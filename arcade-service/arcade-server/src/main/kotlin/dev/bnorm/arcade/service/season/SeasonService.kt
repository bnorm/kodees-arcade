package dev.bnorm.arcade.service.season

import dev.bnorm.arcade.service.Service
import dev.bnorm.arcade.service.api.ParticipantCreateRequest
import dev.bnorm.arcade.service.api.ParticipantId
import dev.bnorm.arcade.service.api.ParticipantResponse
import dev.bnorm.arcade.service.api.SeasonCreateRequest
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.bnorm.arcade.service.repo.DriverRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class SeasonService(
    private val seasons: SeasonRepository,
    private val drivers: DriverRepository,
) : Service {
    suspend fun getSeasons(): List<SeasonResponse> {
        return seasons.getSeasons().map { it.toResponse() }
    }

    suspend fun getSeason(seasonId: SeasonId): SeasonResponse? {
        return seasons.getSeason(seasonId)?.toResponse()
    }

    suspend fun createSeason(request: SeasonCreateRequest): SeasonResponse {
        return seasons.createSeason(request.name).toResponse()
    }

    suspend fun getParticipants(seasonId: SeasonId): List<ParticipantResponse> {
        // TODO does the season exist?
        return seasons.getParticipants(seasonId).map { it.toResponse() }
    }

    suspend fun createParticipant(seasonId: SeasonId, request: ParticipantCreateRequest): ParticipantResponse? {
        val versionEntity = drivers.getDriverVersion(request.driverId, request.version) ?: return null
        return seasons.createParticipant(seasonId, versionEntity.id).toResponse()
    }

    suspend fun getParticipant(seasonId: SeasonId, participantId: ParticipantId): ParticipantResponse? {
        return seasons.getParticipant(seasonId, participantId)?.toResponse()
    }

    suspend fun deleteParticipants(seasonId: SeasonId, participantId: ParticipantId): Boolean {
        return seasons.deleteParticipant(seasonId, participantId)
    }

    private fun SeasonEntity.toResponse(): SeasonResponse {
        return SeasonResponse(
            id = this.id,
            name = this.name,
        )
    }

    private fun ParticipantEntity.toResponse(): ParticipantResponse {
        return ParticipantResponse(
            id = this.id,
            seasonId = this.seasonId,
            driverId = this.driverId,
            name = this.name,
            version = this.version,
        )
    }
}
