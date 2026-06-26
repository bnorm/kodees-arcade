@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        mainRun {
            mainClass = "dev.bnorm.arcade.rally.Main_jvmKt"
        }
    }

    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "arcade.js"

                val proxy = KotlinWebpackConfig.DevServer.Proxy(
                    context = mutableListOf("/api/**"),
                    target = "http://localhost:8080",
                )

                // TODO: use dsl after KT-32016 will be fixed
                devServer = devServer?.copy(
                    proxy = (devServer?.proxy.orEmpty() + proxy).toMutableList(),
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":arcade-engine"))
            implementation(project(":arcade-samples"))
            implementation(project(":arcade-service:arcade-client"))

            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")

            implementation("io.github.vinceglb:filekit-core:0.8.7")
            implementation("io.github.vinceglb:filekit-compose:0.8.7")
        }
        jvmMain.dependencies {
            implementation("ai.tegmentum:wasmtime4j-jni:45.0.1-1.1.5")

            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.6.5")
        }
    }
}
