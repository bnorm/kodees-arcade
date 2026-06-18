@file:OptIn(ExperimentalComposeUiApi::class)

package dev.bnorm.arcade.rally

import ai.tegmentum.wasmtime4j.*
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bnorm.arcade.rally_cartridge.generated.resources.Res
import dev.bnorm.arcade.rally_cartridge.generated.resources.track
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// !!!
// This whole file is me just messing around to get something working that I can test.
// !!!

fun main() {
    val path =
        Path.of("games/rally/rally-sample/build/compileSync/wasmWasi/main/productionExecutable/kotlin/kodees-arcade-games-rally-rally-sample.wasm")
    val track = Json.decodeFromString<Track>(ClassLoader.getSystemResource("track.json")!!.readText())

    val trackWidth = 1024
    val trackHeight = 768

    val scope = CoroutineScope(Dispatchers.Default)

    val game = scope.game(
        track = track,
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        paths = listOf(
            "Kodee 0" to path,
            "Kodee 1" to path,
            "Kodee 2" to path,
            "Kodee 3" to path,
            "Kodee 4" to path,
            "Kodee 5" to path,
        )
    )

    val desiredFps = 60
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Rally",
            state = rememberWindowState(width = trackWidth.dp, height = trackHeight.dp + 32.dp)
        ) {
            var state by remember { mutableStateOf<RallyGameState?>(null) }

            LaunchedEffect(Unit) {
                // Let the render load and pause for a little bit.
                state = game.receive()
                delay(1.seconds)

                val frameDelay = (1.0 / desiredFps).seconds
                require(frameDelay.inWholeMilliseconds > 0)
                val startTime = TimeSource.Monotonic.markNow()
                var targetTime = 0.seconds

                while (isActive) {
                    val next = game.receive()

                    val currentTime = startTime.elapsedNow()
                    val delay = targetTime - currentTime
                    if (delay > Duration.ZERO) delay(delay)
                    targetTime += frameDelay

                    state = next
                }
            }

            Box(
                Modifier.graphicsLayer(
                    scaleX = 2f,
                    scaleY = 2f,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
            ) {
                Image(
                    painter = painterResource(Res.drawable.track),
                    contentDescription = null,
                )

                Canvas(Modifier.matchParentSize()) {
                    for (tank in state?.racers?.values.orEmpty()) {
                        val x = tank.x.toFloat()
                        val y = size.height - tank.y.toFloat()
                        rotate(
                            degrees = 90f - tank.heading.toRelative().degrees.toFloat(),
                            pivot = Offset(x, y)
                        ) {
                            drawRect(Color.Red, Offset(x - 8.0f, y - 16.0f), Size(16.0f, 32.0f))
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
    // TODO hack to make MutableState work...
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
    var checkpoint: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.game(
    track: Track,
    trackWidth: Int,
    trackHeight: Int,
    paths: List<Pair<String, Path>>,
): ReceiveChannel<RallyGameState> = produce {
    val gameState = RallyGameState(
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        finished = false,
        time = 0,
        racers = buildMap {
            for ((index, path) in paths.withIndex()) {
                val position = track.pole_positions[index]
                put(
                    path.first, RallyRacerState(
                        x = position.position.x,
                        y = trackHeight - position.position.y,
                        heading = Angle.ofDegrees(position.rotation.degrees.toDouble()),
                    )
                )
            }
        }
    )

    withEngine { engine ->
        val racers = paths.map { (name, path) ->
            val racerState = gameState.racers.getValue(name)
            val linker = createRacerLinker(engine, racerState)
            WasmRacer.create(engine, linker, path.readBytes(), name) to racerState
        }

        for ((racer, _) in racers) {
            racer.onRace(track, trackHeight)
        }

        while (!gameState.finished) {
            // Allow racers to manipulate controls.
            for ((racer, racerState) in racers) {
                racer.move(gameState, racerState)
            }

            // Update game state.
            update(gameState, track)
            send(gameState)
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
    private val onRaceFunction: WasmFunction,
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
                onRaceFunction = instance.getFunction("onRace").orElseThrow(),
            )

            return racer
        }
    }

    fun move(gameState: RallyGameState, carState: RallyRacerState) {
        memory.ensurePages(64)

        // TODO would it be better to use Protobuf instead?
        memory.writeInt64(0, gameState.time)
        memory.writeFloat64(8, carState.x)
        memory.writeFloat64(16, carState.y)
        memory.writeFloat64(24, carState.heading.radians)
        memory.writeFloat64(32, carState.speed)
        memory.writeInt32(40, carState.checkpoint)

        moveFunction.callVoid()
    }

    fun onRace(track: Track, trackHeight: Int) {
        memory.ensurePages(64)

        // TODO would it be better to use Protobuf instead?
        memory.writeInt32(0, track.checkpoints.size)
        var offset = 4L
        for (checkpoint in track.checkpoints) {
            memory.writeFloat64(offset + 0, checkpoint.start.x)
            memory.writeFloat64(offset + 8, trackHeight - checkpoint.start.y)
            memory.writeFloat64(offset + 16, checkpoint.end.x)
            memory.writeFloat64(offset + 24, trackHeight - checkpoint.end.y)
            offset += 32
        }

        onRaceFunction.callVoid()
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

private fun update(gameState: RallyGameState, track: Track) {
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
        var newX = oldX + newSpeed * cos(newHeading)
        var newY = oldY + newSpeed * sin(newHeading)

        if (newX !in 16.0..gameState.trackWidth - 16.0 || newY !in 16.0..gameState.trackHeight - 16.0) {
            newX = newX.coerceIn(16.0, gameState.trackWidth - 16.0)
            newY = newY.coerceIn(16.0, gameState.trackHeight - 16.0)
            newSpeed = 0.0
        }

        racerState.heading = newHeading
        racerState.speed = newSpeed
        racerState.x = newX
        racerState.y = newY

        // Update target checkpoint.
        val checkpoint = track.checkpoints[racerState.checkpoint]
        val target = checkpoint.toCenter()
        val dx = target.x - newX
        val dy = (gameState.trackHeight - target.y) - newY
        val dist = sqrt(dx * dx + dy * dy)
        val radius = checkpoint.length / 2
        if (dist < radius) {
            racerState.checkpoint = (racerState.checkpoint + 1) % track.checkpoints.size
        }
    }

    // TODO racer collisions
    //  can we do this in none exponential time?
    // sort O(n log n)
    // window traversal O(n^2)
}
