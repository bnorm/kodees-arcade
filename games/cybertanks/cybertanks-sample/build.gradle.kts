import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":games:cybertanks:cybertanks-api"))
        }
    }
}
