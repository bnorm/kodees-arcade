@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")
        }
    }
}

val racers by tasks.registering(Sync::class) {
    into(project.layout.buildDirectory.dir("racers"))

    into("files") {
        from(project(":arcade-samples:kodee").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
            include { it.name.endsWith(".wasm") }
            rename { "Kodee.wasm" }
        }
        from(project(":arcade-samples:snail").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
            include { it.name.endsWith(".wasm") }
            rename { "Snail.wasm" }
        }
    }
}

compose.resources {
    publicResClass = true
    nameOfResClass = "BundledRacers"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = project.layout.buildDirectory.dir("racers")
    )
}

// TODO yuk... custom task?
//  also, should this be a separate project?
tasks.generateResourceAccessorsForCommonMain.configure {
    dependsOn(racers)
}

tasks.copyNonXmlValueResourcesForCommonMain.configure {
    dependsOn(racers)
}

tasks.convertXmlValueResourcesForCommonMain.configure {
    dependsOn(racers)
}
