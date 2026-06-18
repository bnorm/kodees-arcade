import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":games:rally:rally-api"))
        }
    }
}
