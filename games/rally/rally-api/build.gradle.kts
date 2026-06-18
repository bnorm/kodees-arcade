import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi()

    compilerOptions {
        optIn.add("kotlin.wasm.ExperimentalWasmInterop")
        optIn.add("kotlin.wasm.unsafe.UnsafeWasmMemoryApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-agent"))
        }
    }
}
