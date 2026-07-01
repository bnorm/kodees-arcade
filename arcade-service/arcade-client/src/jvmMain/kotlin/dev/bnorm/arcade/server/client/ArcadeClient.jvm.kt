package dev.bnorm.arcade.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.time.Duration.Companion.seconds

actual fun ArcadeClient(host: String, port: Int?): ArcadeClient {
    return HttpArcadeClient(
        host = host,
        port = port,
        baseHttpClient = HttpClient(OkHttp) {
            engine {
                config {
                    // Prevent read timeouts for long-lived SSE connections.
                    readTimeout(60.seconds)
                    connectTimeout(60.seconds)
                }
            }
        },
    )
}
