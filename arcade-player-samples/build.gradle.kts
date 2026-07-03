@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.resources)
        }
    }
}

val drivers by tasks.registering(Sync::class) {
    into(project.layout.buildDirectory.dir("drivers"))

    into("files") {
        from(project(":arcade-player-samples:kodee").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
            include { it.name.endsWith(".wasm") }
            rename { "Kodee.wasm" }
        }
        from(project(":arcade-player-samples:snail").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
            include { it.name.endsWith(".wasm") }
            rename { "Snail.wasm" }
        }
    }
}

compose.resources {
    publicResClass = true
    nameOfResClass = "BundledDrivers"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = project.layout.buildDirectory.dir("drivers")
    )
}

// TODO yuk... custom task?
//  also, should this be a separate project?
tasks.generateResourceAccessorsForCommonMain.configure {
    dependsOn(drivers)
}

tasks.copyNonXmlValueResourcesForCommonMain.configure {
    dependsOn(drivers)
}

tasks.convertXmlValueResourcesForCommonMain.configure {
    dependsOn(drivers)
}
