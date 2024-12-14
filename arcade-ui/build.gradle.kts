import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose-hot-reload")
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

            api(compose.runtime)
            api(compose.material)
            api(compose.components.resources)

            api("io.github.vinceglb:filekit-core:0.8.7")
            api("io.github.vinceglb:filekit-compose:0.8.7")
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}
