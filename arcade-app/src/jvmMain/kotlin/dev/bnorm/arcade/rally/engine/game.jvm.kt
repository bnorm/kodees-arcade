package dev.bnorm.arcade.rally.engine

import ai.tegmentum.wasmtime4j.*
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Point
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.Velocity
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalCoroutinesApi::class)
actual fun CoroutineScope.game(
    track: Track,
    paths: Map<String, PlatformFile>,
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

        send(gameState)

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

private fun createRacerLinker(
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

@OptIn(ExperimentalSerializationApi::class)
private class WasmRacer(
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
