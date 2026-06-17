import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.gradleup.shadow")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":games:cybertanks:cybertanks-api"))

            implementation(project(":arcade-engine"))
            implementation(project(":arcade-render"))

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")

            implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
        }
    }
}

val cartridge by tasks.registering(ShadowJar::class) {
    archiveClassifier = "cartridge"

    val jvmJar = tasks.named<Jar>("jvmJar")
    dependsOn(jvmJar)
    from(jvmJar.map { it.archiveFile })

    val target = kotlin.targets.getByName("jvm")
    configurations = listOf(target.compilations["main"].runtimeDependencyFiles)
}
tasks.assemble.configure { dependsOn(cartridge) }
