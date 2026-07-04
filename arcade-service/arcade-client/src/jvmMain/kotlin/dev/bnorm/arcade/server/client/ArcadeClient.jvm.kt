package dev.bnorm.arcade.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.time.Duration.Companion.seconds
import okhttp3.OkHttpClient

actual fun ArcadeClient(
    host: String,
    port: Int?,
    secure: Boolean,
): ArcadeClient {
    return ArcadeClient(host, port, secure, preconfigured = null)
}

fun ArcadeClient(
    host: String,
    port: Int?,
    secure: Boolean = false,
    preconfigured: OkHttpClient?
): ArcadeClient {
    return HttpArcadeClient(
        host = host,
        port = port,
        secure = secure,
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
