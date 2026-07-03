package dev.bnorm.arcade.service

import dev.bnorm.arcade.service.repo.Repository
import dev.bnorm.arcade.service.route.Router
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.createGraph
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.zstd.zstdStandard
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import org.jetbrains.exposed.v1.core.Slf4jSqlDebugLogger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

fun main(args: Array<String>) {
    System.setProperty("kotlinx.coroutines.debug", "on") // Enable Kotlin coroutines debugging.
    EngineMain.main(args)
}

@Qualifier
annotation class BlobDirectory

@DependencyGraph(AppScope::class)
interface ServerGraph {
    val routers: Set<Router>
    val repositories: Set<Repository>
    val services: Set<Service>

    @Provides
    @BlobDirectory
    private fun providesBlobDirectory(): Path {
        val directory = Paths.get(".blobs")
        directory.deleteRecursively()
        directory.createDirectories()
        return directory
    }

    @Provides
    private fun providesDatabase(): R2dbcDatabase {
        return R2dbcDatabase.connect(
            url = "r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1",
            databaseConfig = R2dbcDatabaseConfig {
                sqlLogger = Slf4jSqlDebugLogger
            }
        )
    }
}

@Suppress("unused") // Loaded by Ktor
suspend fun Application.module() {
    val graph = createGraph<ServerGraph>()
    for (repository in graph.repositories) {
        repository.migrate()
    }
    for (service in graph.services) {
        service.initialize()
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate(4)
    }

    install(CallLogging) {
        callIdMdc("call-id")
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> call.respondText(
                    text = "400: ${cause.message}",
                    status = HttpStatusCode.BadRequest
                )

                else -> call.respondText(
                    text = "500: ${cause.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(Compression) {
        zstdStandard()
    }

    install(SSE)

    routing {
        for (router in graph.routers) {
            router.route()
        }
    }
}
