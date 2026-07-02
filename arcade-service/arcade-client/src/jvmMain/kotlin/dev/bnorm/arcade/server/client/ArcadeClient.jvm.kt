package dev.bnorm.arcade.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.time.Duration.Companion.seconds
import okhttp3.OkHttpClient

actual fun ArcadeClient(host: String, port: Int?): ArcadeClient {
    return ArcadeClient(host, port, preconfigured = null)
}

fun ArcadeClient(host: String, port: Int?, preconfigured: OkHttpClient?): ArcadeClient {
    return HttpArcadeClient(
        host = host,
        port = port,
        baseHttpClient = HttpClient(OkHttp) {
            engine {
                this.preconfigured = preconfigured
                config {
                    // Prevent read timeouts for long-lived SSE connections.
                    readTimeout(60.seconds)
                    connectTimeout(60.seconds)
                }
            }
        },
    )
}
