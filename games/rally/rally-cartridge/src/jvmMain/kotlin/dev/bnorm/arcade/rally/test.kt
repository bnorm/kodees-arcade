package dev.bnorm.arcade.rally

import ai.tegmentum.wasmtime4j.*
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun main() {
    val path =
        Path.of("games/rally/rally-sample/build/compileSync/wasmWasi/main/productionExecutable/kotlin/kodees-arcade-games-rally-rally-sample.wasm")
    val trackWidth = 1600
    val trackHeight = 1200

    val scope = CoroutineScope(Dispatchers.Default)

    val game = game(
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        paths = listOf(
            "Kodee 0" to path,
            "Kodee 1" to path,
            "Kodee 2" to path,
            "Kodee 3" to path,
        )
    )
        // 10 Kodees seems to get around 13,000 EPS
        .withEmitsPerSecond(30)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = RallyGameState(
                trackWidth = trackWidth,
                trackHeight = trackHeight,
                finished = false,
                time = 0,
                racers = emptyMap()
            )
        )

    application {
        Window(::exitApplication, title = "Rally") {
            val state by game.collectAsState()

            with(LocalDensity.current) {
                Canvas(Modifier.requiredSize(trackWidth.toDp(), trackHeight.toDp()).border(1.0.dp, Color.Red)) {
                    // Draw background.
                    drawRect(color = Color.Black, topLeft = Offset.Zero, size = size)

                    // Draw tanks.
                    for (tank in state.racers.values) {
                        val x = tank.x.toFloat()
                        val y = size.height - tank.y.toFloat()
                        rotate(
                            degrees = tank.heading.toRelative().degrees.toFloat(),
                            pivot = Offset(x, y)
                        ) {
                            drawRect(Color.Red, Offset(x - 16.0f, y - 16.0f), Size(32.0f, 32.0f))
                        }
                    }
                }
            }
        }
    }
}

class RallyGameState(
    val trackWidth: Int,
    val trackHeight: Int,
    var finished: Boolean,
    var time: Long,
    val racers: Map<String, RallyRacerState>,
) {
    // TODO hack to make StateFlow work...
    override fun equals(other: Any?): Boolean {
        return false
    }
}

class RallyRacerState(
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var steering: Double = 0.0,
    var throttle: Double = 0.0,
)

fun game(
    trackWidth: Int,
    trackHeight: Int,
    paths: List<Pair<String, Path>>,
): Flow<RallyGameState> = flow {
    val gameState = RallyGameState(
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        finished = false,
        time = 0,
        racers = paths.associate { (name, _) ->
            name to RallyRacerState(
                x = Random.nextDouble(250.0, 750.0),
                y = Random.nextDouble(250.0, 750.0),
                heading = Angle.ofDegrees(Random.nextDouble(360.0)),
            )
        }
    )

    withEngine { engine ->
        val racers = paths.map { (name, path) ->
            val racerState = gameState.racers.getValue(name)
            val linker = createRacerLinker(engine, racerState)
            WasmRacer.create(engine, linker, path.readBytes(), name) to racerState
        }

        while (!gameState.finished) {
            // Allow racers to manipulate controls.
            for ((racer, racerState) in racers) {
                racer.move(gameState, racerState)
            }

            // Update game state.
            update(gameState)
            emit(gameState)
        }
    }
}

fun createRacerLinker(
    engine: Engine,
    racerState: RallyRacerState
): Linker<WasiContext> {
    // TODO is it correct to use WasiContext here?
    val linker: Linker<WasiContext> = WasiLinkerUtils.createLinker(engine)

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(racerState.throttle) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (throttle) -> racerState.throttle = throttle.asDouble() },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(racerState.steering) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (steering) -> racerState.steering = steering.asDouble() },
    )

    return linker
}

interface Racer : AutoCloseable

class WasmRacer(
    val name: String,
    val module: Module,
    val store: Store,
    val instance: Instance,
    private val memory: WasmMemory,
    private val moveFunction: WasmFunction,
) : Racer {

    companion object {
        fun create(
            engine: Engine,
            linker: Linker<WasiContext>,
            racer: ByteArray,
            name: String,
        ): WasmRacer {
            val module = engine.compileModule(racer)
            val store = engine.createStore()
            val instance = linker.instantiate(store, module)

            val racer = WasmRacer(
                name = name,
                module = module,
                store = store,
                instance = instance,
                memory = instance.defaultMemory.orElseThrow(),
                moveFunction = instance.getFunction("move").orElseThrow(),
            )

            return racer
        }
    }

    fun move(gameState: RallyGameState, carState: RallyRacerState) {
        memory.ensurePages(64)
        memory.writeInt64(0, gameState.time)
        memory.writeFloat64(1 * 8, carState.x)
        memory.writeFloat64(2 * 8, carState.y)
        memory.writeFloat64(3 * 8, carState.heading.radians)
        memory.writeFloat64(4 * 8, carState.speed)

        moveFunction.callVoid()
    }

    override fun close() {
        module.close()
        store.close()
        instance.close()
    }
}

private fun WasmMemory.ensurePages(pages: Int) {
    if (size < pages) {
        grow(pages - size)
    }
}

private inline fun withEngine(block: (engine: Engine) -> Unit) {
    WasmRuntimeFactory.create().use { runtime ->
        runtime.createEngine(
            EngineConfig.forSize()
                .wasmFunctionReferences(true)
                .wasmGc(true)
                .wasmExceptions(true)
                .wasmComponentModel(true)
        ).use { engine ->
            block(engine)
        }
    }
}

private fun update(gameState: RallyGameState) {
    gameState.time++

    for (racerState in gameState.racers.values) {
        val steering = racerState.steering
        val throttle = racerState.throttle

        val oldHeading = racerState.heading
        val oldSpeed = racerState.speed
        val oldX = racerState.x
        val oldY = racerState.y

        // TODO consider traction of track
        val newHeading = simulateHeading(oldHeading, oldSpeed, steering, traction = 1.0)
        var newSpeed = simulateSpeed(oldSpeed, throttle)
        var newX = oldX + newSpeed * sin(newHeading)
        var newY = oldY + newSpeed * cos(newHeading)

        if (newX !in 16.0..gameState.trackWidth - 16.0 || newY !in 16.0..gameState.trackHeight - 16.0) {
            newX = newX.coerceIn(16.0, gameState.trackWidth - 16.0)
            newY = newY.coerceIn(16.0, gameState.trackHeight - 16.0)
            newSpeed = 0.0
        }

        racerState.heading = newHeading
        racerState.speed = newSpeed
        racerState.x = newX
        racerState.y = newY
    }

    // TODO racer collisions
    //  can we do this in none exponential time?
    // sort O(n log n)
    // window traversal O(n^2)
}

fun <T> Flow<T>.withEmitsPerSecond(desiredEps: Int): Flow<T> {
    val frameDelay = (1.0 / desiredEps).seconds
    require(frameDelay.inWholeMilliseconds > 0)

    val upstream = this
    return flow {
        val startTime = TimeSource.Monotonic.markNow()
        var targetTime = 0.seconds

        emitAll(upstream.onEach {
            val currentTime = startTime.elapsedNow()
            if (frameDelay.inWholeMilliseconds > 0) {
                // Control FPS by delaying until target time for next frame.
                delay(targetTime - currentTime)
                targetTime += frameDelay
            }
        })
    }
}
