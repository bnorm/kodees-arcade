package dev.bnorm.arcade.gradle.player

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class ArcadeDriverExportTask : DefaultTask() {
    @get:Input
    @get:Optional
    val className = project.objects.property(String::class.java)

    @get:OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("generated/arcade")

    @TaskAction
    fun perform() {
        if (!className.isPresent) {
            throw IllegalArgumentException(
                """
                Driver class name must be configured:
                
                ```
                arcade {
                    className = "fq.name.of.DriverClass"
                }
                ```
                """.trimIndent()
            )
        }

        val directory = outputDir.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()
        val file = directory.resolve("export.kt")
        file.writeText(
            """
            @file:OptIn(dev.bnorm.arcade.driver.internal.DriverExport::class, ExperimentalWasmInterop::class)
            
            private val driver = ${className.get()}
            
            /**
             * Wasm exported function used by the game engine to call our driver.
             */
            @WasmExport("bnorm:arcade/driver#on-race")
            fun onRace(
                trackWidth: Double,
                trackHeight: Double,
                checkpointsPtr: Int,
                checkpointsCount: Int,
                positionsPtr: Int,
                positionsCount: Int,
                laps: Int
            ) {
                dev.bnorm.arcade.driver.internal.driverOnRace(
                    driver,
                    trackWidth,
                    trackHeight,
                    checkpointsPtr,
                    checkpointsCount,
                    positionsPtr,
                    positionsCount,
                    laps,
                )
            }
            
            /**
             * Wasm exported function used by the game engine to call our driver.
             */
            @WasmExport("bnorm:arcade/driver#on-turn")
            fun onTurn(
                time: Long,
                x: Double,
                y: Double,
                heading: Double,
                speed: Double,
                lap: Int,
                nextCheckpoint: Int,
            ) {
                dev.bnorm.arcade.driver.internal.driverOnTurn(
                    driver,
                    time,
                    x,
                    y,
                    heading,
                    speed,
                    lap,
                    nextCheckpoint,
                )
            }
            
            /**
             * Wasm exported function used by the game engine to call our driver.
             */
            @WasmExport("bnorm:arcade/driver#on-draw")
            fun onDraw() {
                dev.bnorm.arcade.driver.internal.driverOnDraw(
                    driver
                )
            }
            """.trimIndent()
        )
    }
}
