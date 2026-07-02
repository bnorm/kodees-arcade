plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.compose)
    application
}

dependencies {
    implementation(project(":arcade-service:arcade-client"))
    implementation(libs.ktor.client.engine.okhttp)

    implementation(project(":arcade-machine:arcade-engine"))
    runtimeOnly(libs.slf4j.nop)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.clikt)
    implementation(libs.clikt.markdown)
    implementation(libs.mosaic.runtime)
}

application {
    mainClass.set("dev.bnorm.arcade.worker.MainKt")
}
