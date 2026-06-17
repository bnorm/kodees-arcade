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
            api(project(":arcade-agent"))
        }

        jvmMain.dependencies {
            implementation("ai.tegmentum.webassembly4j:webassembly4j-runtime:1.3.1")
            runtimeOnly("ai.tegmentum.webassembly4j:wasmtime4j-provider:1.3.1")
            runtimeOnly("ai.tegmentum:wasmtime4j-jni:45.0.1-1.1.5")
            // TODO switch to panama when it works
            // runtimeOnly("ai.tegmentum:wasmtime4j-panama:45.0.1-1.1.5")
        }
    }
}
