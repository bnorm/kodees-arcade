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
            implementation(project(":games:rally:rally-api"))

//            implementation(project(":arcade-engine"))
//            implementation(project(":arcade-render"))
//
//            implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")
//
//            implementation("org.jetbrains.compose.foundation:foundation:1.11.1")

            implementation("org.jetbrains.compose.material:material:1.11.1")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        jvmMain.dependencies {
            implementation("ai.tegmentum:wasmtime4j-jni:45.0.1-1.1.5")

            implementation(compose.desktop.currentOs)
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
