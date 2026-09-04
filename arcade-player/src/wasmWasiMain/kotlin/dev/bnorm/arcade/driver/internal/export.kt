@file:OptIn(
    UnsafeWasmMemoryApi::class,
    ComponentModelInternalApi::class,
)

package dev.bnorm.arcade.driver.internal

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Driver
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.Track
import dev.bnorm.arcade.driver.canvas.internal.ImportCanvas
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.geometry.Vector
import kotlin.wasm.unsafe.ComponentModelInternalApi
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.freeAllComponentModelReallocAllocatedMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is designed only for exporting functions to the Wasm module."
)
annotation class DriverExport

/**
 * Helper function to use Wasm memory for a [Race].
 */
@DriverExport
fun driverOnRace(
    driver: Driver,
    trackWidth: Double,
    trackHeight: Double,
    checkpointsPtr: Int,
    checkpointsCount: Int,
    positionsPtr: Int,
    positionsCount: Int,
    laps: Int,
) {
    freeAllComponentModelReallocAllocatedMemory()
    withScopedMemoryAllocator {
        val checkpoints = List(checkpointsCount) { i ->
            val base = Pointer(((checkpointsPtr) + (i * 32)).toUInt())
            Segment(
                Point(
                    (base + 0).loadDouble(),
                    (base + 8).loadDouble(),
                ),
                Point(
                    (base + 16).loadDouble(),
                    (base + 24).loadDouble(),
                ),
            )
        }

        val positions = List(positionsCount) { i1 ->
            val base = Pointer(((positionsPtr) + (i1 * 24)).toUInt())

            Position(
                Point(
                    (base + 0).loadDouble(),
                    (base + 8).loadDouble(),
                ),
                Angle.ofRadians(
                    (base + 16).loadDouble(),
                ),
            )
        }

        val track = Track(
            trackWidth,
            trackHeight,
            checkpoints,
            positions,
        )

        val race = Race(track, laps)

        driver.onRace(race)
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Car] and [Controls].
 */
@DriverExport
fun driverOnTurn(
    driver: Driver,
    namePtr: Int,
    nameLength: Int,
    time: Long,
    x: Double,
    y: Double,
    heading: Double,
    speed: Double,
    lap: Int,
    nextCheckpoint: Int,
) {
    freeAllComponentModelReallocAllocatedMemory()
    withScopedMemoryAllocator {
        driver.onTurn(
            Car(
                name = Pointer(namePtr.toUInt()).loadString(nameLength),
                time = time,
                location = Point(x, y),
                velocity = Vector(Angle.ofRadians(heading), speed),
                lap = lap,
                nextCheckpoint = nextCheckpoint,
            ),
            ImportControls
        )
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Car].
 */
@DriverExport
fun driverOnCar(
    driver: Driver,
    namePtr: Int,
    nameLength: Int,
    time: Long,
    x: Double,
    y: Double,
    heading: Double,
    speed: Double,
    lap: Int,
    nextCheckpoint: Int,
) {
    freeAllComponentModelReallocAllocatedMemory()
    withScopedMemoryAllocator {
        driver.onCar(
            Car(
                name = Pointer(namePtr.toUInt()).loadString(nameLength),
                time = time,
                location = Point(x, y),
                velocity = Vector(Angle.ofRadians(heading), speed),
                lap = lap,
                nextCheckpoint = nextCheckpoint,
            )
        )
    }
}

/**
 * Helper function to use Wasm memory and imported host functions for a [Canvas].
 */
@DriverExport
fun driverOnDraw(driver: Driver) {
    freeAllComponentModelReallocAllocatedMemory()
    withScopedMemoryAllocator {
        driver.onDraw(ImportCanvas)
    }
}

private fun Pointer.loadDouble(): Double = Double.fromBits(loadLong())
private fun Pointer.loadByteArray(size: Int): ByteArray = ByteArray(size) { i -> (this + i).loadByte() }
private fun Pointer.loadString(length: Int): String = loadByteArray(length).decodeToString()
