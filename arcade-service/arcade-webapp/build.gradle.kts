@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("dev.zacsweers.metro")
}

kotlin {
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "webapp.js"

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
            implementation(project(":arcade-service:arcade-client"))

            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")
            implementation("io.github.vinceglb:filekit-compose:0.8.7")

            implementation("app.softwork:routing-compose:0.5.0")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
    }
}
