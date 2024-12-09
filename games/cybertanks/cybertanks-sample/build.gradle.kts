import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":games:cybertanks:cybertanks-api"))
        }
    }
}
