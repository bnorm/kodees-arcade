import io.ktor.plugin.features.DockerImageRegistry

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.metro)
    application
}

dependencies {
    implementation(project(":arcade-service:arcade-api"))

    implementation(project(":arcade-player"))
    implementation(project(":arcade-machine:arcade-engine"))

    implementation(dependencies.platform(libs.ktor.bom))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.configYaml)

    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.r2dbc.h2)

    implementation(libs.logback)
}

application {
    mainClass.set("dev.bnorm.arcade.service.MainKt")
}

tasks.processResources.configure {
    if (System.getenv("CI") == "true") {
        from(project(":arcade-service:arcade-webapp").tasks.named("wasmJsBrowserDistribution")) {
            into("webapp")
        }
    }
    from(project(":arcade-samples").tasks.getByName("racers")) {
        into("racers")
    }
}

ktor {
    docker {
        jreVersion = JavaVersion.VERSION_21
        localImageName = "arcade-server"

        externalRegistry = DockerImageRegistry.externalRegistry(
            username = providers.environmentVariable("GITHUB_USERNAME"),
            password = providers.environmentVariable("GITHUB_PASSWORD"),
            hostname = provider { "ghcr.io" },
            namespace = provider { "bnorm" },
            project = provider { "arcade-server" },
        )
    }
}
