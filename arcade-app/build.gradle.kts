import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass = "dev.bnorm.arcade.rally.TestKt"
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":arcade-agent"))

            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

            implementation("io.github.vinceglb:filekit-core:0.8.7")
            implementation("io.github.vinceglb:filekit-compose:0.8.7")
        }
        jvmMain.dependencies {
            implementation("ai.tegmentum:wasmtime4j-jni:45.0.1-1.1.5")

            implementation(compose.desktop.currentOs)
        }
    }
}
