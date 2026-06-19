@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmWasi {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":arcade-samples:shared"))
        }
    }
}
