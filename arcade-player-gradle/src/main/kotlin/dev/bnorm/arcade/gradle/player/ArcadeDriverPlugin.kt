package dev.bnorm.arcade.gradle.player

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper

class ArcadeDriverPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Configure Wasm export generation task.
        val extension = target.extensions.create("arcade", ArcadeDriverExtension::class.java)
        val task = target.tasks.register("arcadeDriverExport", ArcadeDriverExportTask::class.java) {
            it.className.set(extension.className)
        }

        // Configure project defaults for Wasm.
        target.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        target.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
            @OptIn(ExperimentalWasmDsl::class)
            kotlin.wasmWasi {
                binaries.executable()
            }

            kotlin.sourceSets.named("wasmWasiMain").configure { sourceSet ->
                sourceSet.dependencies {
                    // TODO use build config to dynamically apply the correct plugin version.
                    implementation("dev.bnorm.arcade:arcade-player")
                }

                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                sourceSet.generatedKotlin.srcDir(task)
                sourceSet.kotlin.srcDir("src")
                sourceSet.kotlin.srcDir("src")
            }
        }

        val sync = target.tasks.register("arcadeDriverSync", Sync::class.java) {
            it.into(target.layout.buildDirectory.dir("drivers"))
            it.from(target.tasks.named("compileProductionExecutableKotlinWasmWasi")) {
                it.include { it.name.endsWith(".wasm") }
                it.rename { "${extension.className.get().substringAfterLast(".")}.wasm" }
            }
        }

        target.tasks.named("assemble").configure {
            it.dependsOn(sync)
        }
    }
}
