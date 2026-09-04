@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.powerassert)
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }
    wasmWasi()

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-geometry"))
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation("com.willowtreeapps.opentest4k:opentest4k:1.3.0")
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
