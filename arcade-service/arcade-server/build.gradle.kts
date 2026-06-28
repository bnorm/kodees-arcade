import io.ktor.plugin.features.DockerImageRegistry

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    application
}

dependencies {
    implementation(project(":arcade-service:arcade-api"))

    implementation(project(":arcade-agent"))
    implementation(project(":arcade-engine"))

    implementation(dependencies.platform("io.ktor:ktor-bom:3.5.0"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-config-yaml")

    val exposed_version = "1.3.0"
    implementation("org.jetbrains.exposed:exposed-core:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-r2dbc:${exposed_version}")

    implementation("io.r2dbc:r2dbc-h2:1.1.0.RELEASE")

    implementation("ch.qos.logback:logback-classic:1.5.35")
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
