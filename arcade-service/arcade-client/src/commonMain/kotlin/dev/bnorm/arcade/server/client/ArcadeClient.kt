package dev.bnorm.arcade.server.client

import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackResponse
import kotlinx.coroutines.flow.Flow

expect fun ArcadeClient(host: String = "localhost", port: Int = 8080): ArcadeClient

interface ArcadeClient : AutoCloseable {
    suspend fun getRaces(): List<RaceResponse>
    suspend fun getRace(id: RaceId): RaceResponse
    suspend fun createRace(request: RaceCreateRequest): RaceResponse

    // TODO find the best way to deal with the first event being the RaceResponse
    fun streamRace(request: RaceCreateRequest): Flow<String>

    // TODO find the best way to deal directly with bytes without exposing ktor
    fun downloadRace(id: RaceId): Flow<String>

    suspend fun getRacers(): List<RacerResponse>

    suspend fun getTracks(): List<TrackResponse>
}
