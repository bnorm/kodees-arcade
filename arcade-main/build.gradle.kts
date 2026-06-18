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
            api(project(":arcade-runner"))
            api(project(":arcade-app"))

            api("io.github.vinceglb:filekit-core:0.8.7")
            api("io.github.vinceglb:filekit-compose:0.8.7")
        }

        jvmMain.dependencies {
            api(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        nativeDistributions {
            linux {
                modules("jdk.security.auth")
            }
        }
    }
}
