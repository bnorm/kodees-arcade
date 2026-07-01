package dev.bnorm.arcade.server.client

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.RacerCreateRequest
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.api.Version
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

expect fun ArcadeClient(host: String = "localhost", port: Int? = null): ArcadeClient

interface ArcadeClient : AutoCloseable {
    suspend fun getRaces(): List<RaceResponse>
    suspend fun getRace(id: RaceId): RaceResponse
    suspend fun createRace(request: RaceCreateRequest): RaceResponse

    // TODO find the best way to deal directly with bytes without exposing ktor
    fun downloadRace(id: RaceId): Flow<String>

    suspend fun getRacers(): List<RacerResponse>
    suspend fun createRacer(request: RacerCreateRequest): RacerResponse
    suspend fun getRacer(id: RacerId): RacerResponse
    suspend fun downloadRacerVersion(id: RacerId, version: Version): ByteArray
    suspend fun uploadRacerVersion(id: RacerId, version: Version, bytes: ByteArray): RacerResponse

    suspend fun getTracks(): List<TrackResponse>
    suspend fun getTrack(id: TrackId): TrackResponse
    suspend fun downloadTrack(id: TrackId): ByteArray

    fun listen(): Flow<RaceProcessEvent>

    // TODO is this the best way to upload bytes?
    suspend fun upload(id: RaceId, nonce: Nonce, events: ReceiveChannel<String>): RaceResponse
}
