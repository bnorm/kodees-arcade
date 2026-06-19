package dev.bnorm.arcade.rally

import ai.tegmentum.wasmtime4j.*
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.math.sqrt

val carWidth = 12.0
val carHeight = 16.0
//val impactDist = carHeight / 2.0
val impactDist = (68.0 * 0.4f)
val impactDistSq = impactDist * impactDist

class RallyGameState(
    val trackWidth: Double,
    val trackHeight: Double,
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
    val image: ImageBitmap,
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var steering: Double = 0.0,
    var throttle: Double = 0.0,
    var checkpoint: Int = 0,
    var lap: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.game(
    track: Track,
    paths: Map<String, Path>,
    carImages: List<ImageBitmap>,
): ReceiveChannel<RallyGameState> = produce {
    val carImages = ArrayDeque(carImages.shuffled())

    val gameState = RallyGameState(
        trackWidth = track.width,
        trackHeight = track.height,
        finished = false,
        time = 0,
        racers = buildMap {
            for ((index, path) in paths.entries.withIndex()) {
                val position = track.positions[index]
                put(
                    path.key,
                    RallyRacerState(
                        image = carImages.removeFirst(),
                        x = position.location.x,
                        y = position.location.y,
                        heading = position.heading,
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
            racer.onRace(track)
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

@OptIn(ExperimentalSerializationApi::class)
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
            linker: Linker<*>,
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
        val car = Car(
            time = gameState.time,
            location = Point(carState.x, carState.y),
            velocity = Velocity(carState.heading, carState.speed),
            nextCheckpoint = carState.checkpoint,
        )
        val bytes = ProtoBuf.encodeToByteArray(Car.serializer(), car)

        memory.ensureBytes(4 + bytes.size)
        memory.writeInt32(0, bytes.size)
        memory.writeBytes(4, bytes, 0, bytes.size)

        moveFunction.callVoid()
    }

    fun onRace(track: Track) {
        val bytes = ProtoBuf.encodeToByteArray(Track.serializer(), track)

        memory.ensureBytes(4 + bytes.size)
        memory.writeInt32(0, bytes.size)
        memory.writeBytes(4, bytes, 0, bytes.size)

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

private fun WasmMemory.ensureBytes(byteCount: Int) {
    ensurePages(byteCount / pageSize() + (byteCount % pageSize()).coerceAtMost(1))
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
        val target = checkpoint.center
        val radius = checkpoint.length / 2

        val dx = target.x - racerState.x
        val dy = (target.y) - racerState.y
        val dist = sqrt(dx * dx + dy * dy)
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
    //  should impacts effect speed?
    //    this might make the physics a little more complicated than it needs to be...

    // Only do a single pass...
    // TODO is a little bit of clipping okay?
    for ((i, racer1) in racers.withIndex()) {
        for (j in (i + 1)..<racers.size) {
            val racer2 = racers[j]

            val dx = racer1.x - racer2.x
            val dy = racer1.y - racer2.y
            val distSq = dx * dx + dy * dy
            if (distSq < impactDistSq) {
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