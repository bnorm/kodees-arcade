@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose)
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

            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.filekit.compose)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )

            mainClass = "dev.bnorm.arcade.rally.Main_jvmKt"
            packageName = "Kodee's Arcade"
            macOS {
            }
            windows {
            }
            linux {
                packageName = "kodees-arcade"
            }
        }
    }
}
