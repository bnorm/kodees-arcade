package dev.bnorm.arcade.rally

import ai.tegmentum.wasmtime4j.*
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0

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
    var checkpoint: Int = 0,
    var lap: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.game(
    track: Track,
    trackWidth: Int,
    trackHeight: Int,
    paths: Map<String, Path>,
): ReceiveChannel<RallyGameState> = produce {
    val gameState = RallyGameState(
        trackWidth = trackWidth,
        trackHeight = trackHeight,
        finished = false,
        time = 0,
        racers = buildMap {
            for ((index, path) in paths.entries.withIndex()) {
                val position = track.pole_positions[index]
                put(
                    path.key,
                    RallyRacerState(
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

        send(gameState)

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

    val racers = gameState.racers.values.toList()
    for (racerState in racers) {
        val steering = racerState.steering
        val throttle = racerState.throttle

        val oldHeading = racerState.heading
        val oldSpeed = racerState.speed

        // TODO consider traction of track
        val newHeading = simulateHeading(oldHeading, oldSpeed, steering, traction = 1.0)
        var newSpeed = simulateSpeed(oldSpeed, throttle)
        if (updatePosition(racerState, newSpeed, newHeading, gameState)) {
            newSpeed = 0.0
        }

        racerState.heading = newHeading
        racerState.speed = newSpeed

        // Update target checkpoint.
        val checkpoint = track.checkpoints[racerState.checkpoint]
        val target = checkpoint.toCenter()
        val dx = target.x - racerState.x
        val dy = (gameState.trackHeight - target.y) - racerState.y
        val dist = sqrt(dx * dx + dy * dy)
        val radius = checkpoint.length / 2
        if (dist < radius) {
            racerState.checkpoint += 1
            if (racerState.checkpoint >= track.checkpoints.size) {
                racerState.lap += 1
                racerState.checkpoint = 0
            }
        }
    }

    // TODO optimize racer collisions
    //  better representation for the cars
    //    rotated ovals?
    //    convex polygons?
    //  can we do this in a single pass?
    //  should impacts effect speed?
    //    this might make the physics a little more complicated than it needs to be...
    val impactDistSq = carHeight * carHeight
    do {
        var impact = false
        for ((i, racer1) in racers.withIndex()) {
            for (j in (i + 1)..<racers.size) {
                val racer2 = racers[j]

                val dx = racer1.x - racer2.x
                val dy = racer1.y - racer2.y
                val distSq = dx * dx + dy * dy
                if (distSq < impactDistSq) {
                    impact = true

                    val delta = sqrt(impactDistSq) - sqrt(distSq)
                    val angle = atan2(dy, dx)
                    val impulse = (delta / 2).coerceAtLeast(0.1)
                    if (updatePosition(racer1, impulse, angle, gameState)) {
                        racer1.speed = 0.0
                    }
                    if (updatePosition(racer2, impulse, angle + Angle.HALF_CIRCLE, gameState)) {
                        racer2.speed = 0.0
                    }
                }
            }
        }
    } while (impact)
}

/** @return if impacted with a wall. */
private fun updatePosition(
    racerState: RallyRacerState,
    magnitude: Double,
    heading: Angle,
    gameState: RallyGameState
): Boolean {
    val newX = racerState.x + magnitude * cos(heading)
    val newY = racerState.y + magnitude * sin(heading)
    racerState.x = newX
    racerState.y = newY

    val impactDist = carHeight / 2
    if (
        newX !in impactDist..gameState.trackWidth - impactDist ||
        newY !in impactDist..gameState.trackHeight - impactDist
    ) {
        racerState.x = newX.coerceIn(impactDist, gameState.trackWidth - impactDist)
        racerState.y = newY.coerceIn(impactDist, gameState.trackHeight - impactDist)
        return true
    }

    return false
}