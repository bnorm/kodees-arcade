plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":arcade-service:arcade-client"))
    implementation(project(":arcade-machine:arcade-engine"))
    implementation(libs.ktor.serialization.json)
}

application {
    mainClass.set("dev.bnorm.arcade.worker.MainKt")
}
