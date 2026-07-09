package dev.bnorm.arcade.server.client

import dev.bnorm.arcade.service.api.DriverCreateRequest
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.DriverResponse
import dev.bnorm.arcade.service.api.DriverVersionResponse
import dev.bnorm.arcade.service.api.Nonce
import dev.bnorm.arcade.service.api.ParticipantCreateRequest
import dev.bnorm.arcade.service.api.ParticipantId
import dev.bnorm.arcade.service.api.ParticipantResponse
import dev.bnorm.arcade.service.api.RaceCreateRequest
import dev.bnorm.arcade.service.api.RaceId
import dev.bnorm.arcade.service.api.RaceProcessEvent
import dev.bnorm.arcade.service.api.RaceResponse
import dev.bnorm.arcade.service.api.SeasonCreateRequest
import dev.bnorm.arcade.service.api.SeasonId
import dev.bnorm.arcade.service.api.SeasonRaceCreateRequest
import dev.bnorm.arcade.service.api.SeasonResponse
import dev.bnorm.arcade.service.api.TrackCreateRequest
import dev.bnorm.arcade.service.api.TrackId
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.service.api.Version
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.discardRemaining
import io.ktor.http.ContentType
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal fun HttpArcadeClient(
    host: String,
    port: Int? = null,
    secure: Boolean = false,
    baseHttpClient: HttpClient,
): ArcadeClient {
    return HttpArcadeClient(
        hostUrl = buildUrl {
            this.protocol = if (secure) URLProtocol.HTTPS else URLProtocol.HTTP
            this.host = host
            this.port = port ?: if (host == "localhost") 8080 else DEFAULT_PORT
        },
        baseHttpClient = baseHttpClient,
        json = DefaultJson,
    )
}

internal class HttpArcadeClient(
    private val hostUrl: Url,
    baseHttpClient: HttpClient,
    private val json: Json = DefaultJson,
) : ArcadeClient {
    private val httpClient = baseHttpClient.config {
        install(ContentNegotiation) {
            json(json)
        }

        install(SSE)

        expectSuccess = true
    }

    private val apiUrl = buildUrl {
        takeFrom(hostUrl)
        path("api", "rally")
    }

    private fun apiPath(vararg path: String): Url {
        return buildUrl {
            takeFrom(apiUrl)
            appendPathSegments(path.toList())
        }
    }

    private suspend inline fun <reified R> get(path: String): R {
        return httpClient.get(apiPath(path)).body()
    }

    private suspend inline fun <reified B, reified R> post(path: String, request: B): R {
        return httpClient.post(apiPath(path)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    private suspend fun delete(path: String) {
        httpClient.delete(apiPath(path)).discardRemaining()
    }

    override suspend fun getRaces(): List<RaceResponse> {
        return get("races")
    }

    override suspend fun getRace(id: RaceId): RaceResponse {
        return get("races/$id")
    }

    override suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        return post("races", request)
    }

    override suspend fun resetRace(id: RaceId): RaceResponse {
        return httpClient.put(apiPath("races/$id/reset")).body()
    }

    override fun downloadRace(id: RaceId): Flow<ByteArray> = flow {
        val response = httpClient.get(apiPath("races/$id/download"))
        val channel = response.bodyAsChannel()
        while (channel.awaitContent()) {
            val size = channel.readInt()
            emit(channel.readByteArray(size))
        }
    }

    override suspend fun getDrivers(): List<DriverResponse> {
        return get("drivers")
    }

    override suspend fun createDriver(request: DriverCreateRequest): DriverResponse {
        return post("drivers", request)
    }

    override suspend fun getDriver(id: DriverId): DriverResponse {
        return get("drivers/$id")
    }

    override suspend fun getDriverVersions(id: DriverId): List<DriverVersionResponse> {
        return get("drivers/$id/versions")
    }

    override suspend fun downloadDriverVersion(id: DriverId, version: Version): ByteArray {
        return httpClient.get(apiPath("drivers/$id/versions/${version}/download")).bodyAsBytes()
    }

    override suspend fun uploadDriverVersion(id: DriverId, version: Version, bytes: ByteArray): DriverResponse {
        return httpClient.post(apiPath("drivers/$id/versions/${version}/upload")) {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }.body()
    }

    override suspend fun getTracks(): List<TrackResponse> {
        return get("tracks")
    }

    override suspend fun createTrack(request: TrackCreateRequest): TrackResponse {
        return post("tracks", request)
    }

    override suspend fun getTrack(id: TrackId): TrackResponse {
        return get("tracks/$id")
    }

    override fun listen(): Flow<RaceProcessEvent> = channelFlow {
        httpClient.sse(
            request = {
                url.takeFrom(apiPath("races/listen"))
            }
        ) {
            incoming.collect {
                val data = it.data ?: return@collect
                this@channelFlow.send(json.decodeFromString(RaceProcessEvent.serializer(), data))
            }
        }
    }

    override suspend fun upload(
        id: RaceId,
        nonce: Nonce,
        events: ReceiveChannel<ByteArray>
    ): RaceResponse = coroutineScope {
        val channel = ByteChannel()
        launch {
            channel.use {
                events.consumeEach {
                    channel.writeInt(it.size)
                    channel.writeByteArray(it)
                }
            }
        }

        httpClient.post(apiPath("races/$id/upload")) {
            contentType(ContentType.Application.OctetStream)
            parameter("nonce", nonce)
            setBody(channel)
        }.body()
    }

    override suspend fun getSeasons(): List<SeasonResponse> {
        return get("seasons")
    }

    override suspend fun createSeason(request: SeasonCreateRequest): SeasonResponse {
        return post("seasons", request)
    }

    override suspend fun getSeason(id: SeasonId): SeasonResponse {
        return get("seasons/$id")
    }

    override suspend fun getParticipants(seasonId: SeasonId): List<ParticipantResponse> {
        return get("seasons/$seasonId/participants")
    }

    override suspend fun createParticipant(seasonId: SeasonId, request: ParticipantCreateRequest): ParticipantResponse {
        return post("seasons/$seasonId/participants", request)
    }

    override suspend fun getParticipant(seasonId: SeasonId, participantId: ParticipantId): ParticipantResponse {
        return get("seasons/$seasonId/participants/$participantId")
    }

    override suspend fun removeParticipant(seasonId: SeasonId, participantId: ParticipantId) {
        delete("seasons/$seasonId/participants/$participantId")
    }

    override suspend fun getSeasonRaces(seasonId: SeasonId): List<RaceResponse> {
        return get("seasons/$seasonId/races")
    }

    override suspend fun createSeasonRace(seasonId: SeasonId, request: SeasonRaceCreateRequest): RaceResponse {
        return post("seasons/$seasonId/races", request)
    }

    override fun close() {
        httpClient.close()
        httpClient.engine.close()
    }
}
