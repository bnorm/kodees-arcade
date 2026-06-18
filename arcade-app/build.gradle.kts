import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    sourceSets {
        commonMain.dependencies {
            api(project(":arcade-core"))
            api(project(":arcade-engine"))
            api(project(":arcade-render"))
            api(project(":arcade-agent"))
            api(project(":arcade-runner"))

            api("org.jetbrains.compose.runtime:runtime:1.11.1")
            api("org.jetbrains.compose.material:material:1.11.1")
            api("org.jetbrains.compose.components:components-resources:1.11.1")

            api("io.github.vinceglb:filekit-core:0.8.7")
            api("io.github.vinceglb:filekit-compose:0.8.7")
        }
    }
}
