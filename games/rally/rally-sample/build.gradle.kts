import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi("Kodee") {
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":games:rally:rally-api"))
        }
    }
}

val racers by tasks.registering(Sync::class) {
    into(project.layout.buildDirectory.dir("racers"))
    from(tasks.named("compileProductionExecutableKotlinKodee")) {
        include { it.name.endsWith(".wasm") }
        rename { "Kodee.wasm" }
    }
}
