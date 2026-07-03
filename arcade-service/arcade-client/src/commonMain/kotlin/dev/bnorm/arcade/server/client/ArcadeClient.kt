package dev.bnorm.arcade.server.client

import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.DriverCreateRequest
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.DriverResponse
import dev.bnorm.arcade.service.api.DriverVersionResponse
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.api.Version
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow

expect fun ArcadeClient(
    host: String = "localhost",
    port: Int? = null,
    secure: Boolean = false,
): ArcadeClient

interface ArcadeClient : AutoCloseable {
    suspend fun getRaces(): List<RaceResponse>
    suspend fun getRace(id: RaceId): RaceResponse
    suspend fun createRace(request: RaceCreateRequest): RaceResponse
    suspend fun resetRace(id: RaceId): RaceResponse

    // TODO find the best way to deal directly with bytes without exposing ktor
    fun downloadRace(id: RaceId): Flow<ByteArray>

    suspend fun getDrivers(): List<DriverResponse>
    suspend fun createDriver(request: DriverCreateRequest): DriverResponse
    suspend fun getDriver(id: DriverId): DriverResponse
    suspend fun getDriverVersions(id: DriverId): List<DriverVersionResponse>
    suspend fun downloadDriverVersion(id: DriverId, version: Version): ByteArray
    suspend fun uploadDriverVersion(id: DriverId, version: Version, bytes: ByteArray): DriverResponse

    suspend fun getTracks(): List<TrackResponse>
    suspend fun getTrack(id: TrackId): TrackResponse
    suspend fun downloadTrack(id: TrackId): ByteArray

    fun listen(): Flow<RaceProcessEvent>

    // TODO is this the best way to upload bytes?
    suspend fun upload(id: RaceId, nonce: Nonce, events: ReceiveChannel<ByteArray>): RaceResponse
}
