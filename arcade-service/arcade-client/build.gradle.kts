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
            api(project(":arcade-service:arcade-api"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation(dependencies.platform("io.ktor:ktor-bom:3.5.0"))
            implementation("io.ktor:ktor-client-core")
            implementation("io.ktor:ktor-client-content-negotiation")
            implementation("io.ktor:ktor-serialization-kotlinx-json")
        }
        wasmJsMain.dependencies {
            implementation("io.ktor:ktor-client-js")
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp")
        }
    }
}
