@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    wasmWasi()

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-player"))
        }
    }
}
