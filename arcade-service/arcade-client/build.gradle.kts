@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-service:arcade-api"))
            api(libs.kotlinx.coroutines.core)

            implementation(libs.kotlinx.serialization.json)

            implementation(dependencies.platform(libs.ktor.bom))
            implementation(libs.ktor.client)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.engine.js)
            implementation(libs.kotlinx.browser)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.engine.okhttp)
        }
    }
}
