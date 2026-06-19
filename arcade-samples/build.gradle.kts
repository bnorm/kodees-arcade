@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":arcade-agent"))
        }
    }
}

// TODO create a custom Gradle plugin for all of the following
//  - based on configuration of 'name: String' to 'fqdn: String'.
//  - generate a 'wasmWasi' target based on 'name'.
//  - automatically generate a Wasm export file based on 'fqdn'.
//  - configure a "racers" like task to aggregate all '.wasm' files in an output directory.

kotlin {
    wasmWasi("Kodee") {
        binaries.executable()
    }
}

val racers by tasks.registering(Sync::class) {
    into(project.layout.buildDirectory.dir("racers"))

    kotlin.targets.withType<KotlinWasmWasiTargetDsl>().configureEach {
        val targetName = this.name
        compilations["main"].binaries.matching { it.name == "productionExecutable" }.configureEach {
            from(linkTask) {
                include { it.name.endsWith(".wasm") }
                rename { "$targetName.wasm" }
            }
        }
    }
}
