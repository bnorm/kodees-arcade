package dev.bnorm.arcade.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun ArcadeClient(
    host: String,
    port: Int?,
    secure: Boolean,
): ArcadeClient {
    return HttpArcadeClient(
        host = host,
        port = port,
        secure = secure,
        baseHttpClient = HttpClient(Js),
    )
}
