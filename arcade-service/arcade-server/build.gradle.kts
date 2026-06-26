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
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposed_version}")
    implementation("org.jetbrains.exposed:exposed-json:${exposed_version}")

    implementation("org.postgresql:postgresql:42.7.11")
    implementation("com.h2database:h2:2.4.240")

    implementation("ch.qos.logback:logback-classic:1.5.35")
}

application {
    mainClass.set("dev.bnorm.arcade.rally.MainKt")
}

tasks.processResources.configure {
    from(project(":arcade-samples").tasks.getByName("racers")) {
        into("racers")
    }
}
