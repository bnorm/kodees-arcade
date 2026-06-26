@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-agent"))

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.11.0")

            implementation("io.github.vinceglb:filekit-core:0.8.7")
        }
        jvmMain.dependencies {
            implementation("ai.tegmentum:wasmtime4j-jni:45.0.1-1.1.5")
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.6.5")
        }
    }
}
