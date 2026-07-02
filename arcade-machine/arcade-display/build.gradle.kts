@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-machine:arcade-multicade"))
            api(project(":arcade-service:arcade-client"))

            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.filekit.dialogs.compose)

            implementation(libs.kotlinx.serialization.protobuf)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
