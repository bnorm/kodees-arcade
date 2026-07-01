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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
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
import io.ktor.utils.io.readLineStrict
import io.ktor.utils.io.writeString
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal fun HttpArcadeClient(host: String, port: Int? = null, baseHttpClient: HttpClient): ArcadeClient {
    return HttpArcadeClient(
        hostUrl = buildUrl {
            this.protocol = if (host == "localhost") URLProtocol.HTTP else URLProtocol.HTTPS
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

    override suspend fun getRaces(): List<RaceResponse> {
        return httpClient.get(apiPath("races")).body()
    }

    override suspend fun getRace(id: RaceId): RaceResponse {
        return httpClient.get(apiPath("races/$id")).body()
    }

    override suspend fun createRace(request: RaceCreateRequest): RaceResponse {
        return httpClient.post(apiPath("races")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override fun downloadRace(id: RaceId): Flow<String> = flow {
        val response = httpClient.get(apiPath("races/$id/download"))
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            emit(channel.readLineStrict() ?: break)
        }
    }

    override suspend fun getRacers(): List<RacerResponse> {
        return httpClient.get(apiPath("racers")).body()
    }

    override suspend fun createRacer(request: RacerCreateRequest): RacerResponse {
        return httpClient.post(apiPath("racers")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getRacer(id: RacerId): RacerResponse {
        return httpClient.get(apiPath("racers/$id")).body()
    }

    override suspend fun downloadRacerVersion(id: RacerId, version: Version): ByteArray {
        return httpClient.get(apiPath("racers/$id/download/${version}")).bodyAsBytes()
    }

    override suspend fun uploadRacerVersion(id: RacerId, version: Version, bytes: ByteArray): RacerResponse {
        return httpClient.post(apiPath("racers/$id/upload/${version}")) {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }.body()
    }

    override suspend fun getTracks(): List<TrackResponse> {
        return httpClient.get(apiPath("tracks")).body()
    }

    override suspend fun getTrack(id: TrackId): TrackResponse {
        return httpClient.get(apiPath("tracks/$id")).body()
    }

    override suspend fun downloadTrack(id: TrackId): ByteArray {
        return httpClient.get(apiPath("tracks/$id/download")).body()
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
        events: ReceiveChannel<String>
    ): RaceResponse = coroutineScope {
        val channel = ByteChannel()
        launch {
            channel.use {
                events.consumeEach {
                    channel.writeString(it)
                    channel.writeString("\n")
                }
            }
        }

        httpClient.post(apiPath("races/$id/upload")) {
            contentType(ContentType.Application.OctetStream)
            parameter("nonce", nonce)
            setBody(channel)
        }.body()
    }


    override fun close() {
        httpClient.close()
        httpClient.engine.close()
    }
}
