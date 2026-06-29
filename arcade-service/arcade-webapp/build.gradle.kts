@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.metro)
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

            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.filekit.compose)
            implementation(libs.routing.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
