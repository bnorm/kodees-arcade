import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-core"))
            api(project(":arcade-agent"))
            api(project(":arcade-engine"))
            api(project(":arcade-render"))

            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
    }
}
