package dev.bnorm.arcade.rally.engine

import ai.tegmentum.wasmtime4j.Instance
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.Module
import ai.tegmentum.wasmtime4j.Store
import ai.tegmentum.wasmtime4j.WasmMemory
import ai.tegmentum.wasmtime4j.WasmValue.f64
import ai.tegmentum.wasmtime4j.WasmValue.i32
import ai.tegmentum.wasmtime4j.WasmValue.i64
import ai.tegmentum.wasmtime4j.WasmValueType.F32
import ai.tegmentum.wasmtime4j.WasmValueType.F64
import ai.tegmentum.wasmtime4j.WasmValueType.I32
import ai.tegmentum.wasmtime4j.WasmValueType.I64
import ai.tegmentum.wasmtime4j.func.HostFunction.singleValue
import ai.tegmentum.wasmtime4j.func.HostFunction.voidFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.io.Buffer
import kotlinx.io.DelicateIoApi
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.writeToInternalBuffer

actual suspend fun WasmModule(bytes: ByteArray): WasmModule {
    return WasmtimeWasmModule(engine.compileModule(bytes))
}

private class WasmtimeWasmModule(
    private val module: Module
) : WasmModule {
    override suspend fun createDriver(name: String): WasmDriver {
        return WasmtimeWasmDriver.of(name, module)
    }
}

private class WasmtimeWasmDriver private constructor(
    override val name: String,
) : WasmDriver {
    companion object {
        fun of(name: String, module: Module): WasmtimeWasmDriver {
            val driver = WasmtimeWasmDriver(name)
            val guest = driver.guest

            guest.store = engine.createStore()
            guest.linker = runtime.createLinker<Nothing>(engine).apply {
                defineHostFunction(
                    "wasi_snapshot_preview1", "fd_write",
                    FunctionType(arrayOf(I32, I32, I32, I32), arrayOf(I32)),
                    singleValue { (iovs_len, iovs, fd, nwritten) ->
                        i32(
                            fdWrite(
                                memory = guest.memory,
                                iovs = iovs.asInt(),
                                iovs_len = iovs_len.asInt(),
                                fd = fd.asInt(),
                                nwritten = nwritten.asInt(),
                                stdout = driver.stdout,
                                stderr = driver.stderr,
                            )
                        )
                    },
                )
                defineHostFunction(
                    "wasi_snapshot_preview1", "random_get",
                    FunctionType(arrayOf(I32, I32), arrayOf(I32)),
                    singleValue { (bufLen, bufPtr) ->
                        i32(randomGet(guest.memory, driver.random, bufLen.asInt(), bufPtr.asInt()))
                    },
                )
                defineHostFunction(
                    "wasi_snapshot_preview1", "environ_sizes_get",
                    FunctionType(arrayOf(I32, I32), arrayOf(I32)),
                    singleValue { (environCountPtr, environBufferSizePtr) ->
                        // TODO what environment variables should we expose?
                        guest.memory.writeInt32(environCountPtr.asInt().toLong(), 0)
                        guest.memory.writeInt32(environBufferSizePtr.asInt().toLong(), 0)
                        i32(0)
                    },
                )
                defineHostFunction(
                    "wasi_snapshot_preview1", "environ_get",
                    FunctionType(arrayOf(I32, I32), arrayOf(I32)),
                    singleValue { (environPtr, environBufferPtr) ->
                        // TODO what environment variables should we expose?
                        i32(0)
                    },
                )
                defineHostFunction(
                    "wasi_snapshot_preview1", "proc_exit",
                    FunctionType(arrayOf(I32), arrayOf()),
                    voidFunction { (errorCode) ->
                        // TODO how to exit?
                    },
                )

                defineHostFunction(
                    "bnorm:arcade/controls", "throttle-get",
                    FunctionType(arrayOf(), arrayOf(F64)),
                    singleValue { f64(driver.throttle) },
                )
                defineHostFunction(
                    "bnorm:arcade/controls", "throttle-set",
                    FunctionType(arrayOf(F64), arrayOf()),
                    voidFunction { (throttle) -> driver.throttle = throttle.asDouble() },
                )
                defineHostFunction(
                    "bnorm:arcade/controls", "steering-get",
                    FunctionType(arrayOf(), arrayOf(F64)),
                    singleValue { f64(driver.steering) },
                )
                defineHostFunction(
                    "bnorm:arcade/controls", "steering-set",
                    FunctionType(arrayOf(F64), arrayOf()),
                    voidFunction { (steering) -> driver.steering = steering.asDouble() },
                )

                defineHostFunction(
                    "bnorm:arcade/canvas", "draw",
                    FunctionType(arrayOf(I32, I32, F64, F64, F64, F64, I64, I32, F32), arrayOf()),
                    voidFunction { params ->
                        driver.drawRequests.add(
                            readDrawRequest(
                                params[0].asInt(),
                                params[1].asInt(),
                                params[2].asDouble(),
                                params[3].asDouble(),
                                params[4].asDouble(),
                                params[5].asDouble(),
                                params[6].asLong(),
                                params[7].asInt(),
                                params[8].asFloat()
                            )
                        )
                    },
                )
            }

            guest.instance = guest.linker.instantiate(guest.store, module)
            guest.memory = guest.instance.defaultMemory.orElseThrow()

            return driver
        }
    }

    private val guest = WasmGuest()

    override var steering: Double = 0.0
        private set

    override var throttle: Double = 0.0
        private set

    override val stdout: RawSource
        field = Buffer()

    override val stderr: RawSource
        field = Buffer()

    private val random = Random(Clock.System.now().toEpochMilliseconds())

    private val drawRequests = mutableListOf<DrawRequest>()

    private val onRace by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.getFunction("bnorm:arcade/driver#on-race").getOrNull()!!
    }

    private val onTurn by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.getFunction("bnorm:arcade/driver#on-turn").getOrNull()!!
    }

    private val onDraw by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.getFunction("bnorm:arcade/driver#on-draw").getOrNull()
    }

    override fun onRace(race: Race) {
        val memory = guest.memory
        memory.require(
            race.track.checkpoints.size * 32 +
                race.track.positions.size * 24
        )

        val checkpointsPtr = 0
        for ((i, checkpoint) in race.track.checkpoints.withIndex()) {
            val base = (checkpointsPtr) + (i * 32L)
            memory.writeFloat64(base + 0, checkpoint.start.x)
            memory.writeFloat64(base + 8, checkpoint.start.y)
            memory.writeFloat64(base + 16, checkpoint.end.x)
            memory.writeFloat64(base + 24, checkpoint.end.y)
        }

        val positionsPtr = race.track.checkpoints.size * 32
        for ((i, position) in race.track.positions.withIndex()) {
            val base = (positionsPtr) + (i * 24L)
            memory.writeFloat64(base + 0, position.location.x)
            memory.writeFloat64(base + 8, position.location.y)
            memory.writeFloat64(base + 16, position.heading.radians)
        }

        onRace.call(
            f64(race.track.width),
            f64(race.track.height),
            i32(checkpointsPtr),
            i32(race.track.checkpoints.size),
            i32(positionsPtr),
            i32(race.track.positions.size),
            i32(race.laps),
        )
    }

    override fun onTurn(car: Car) {
        onTurn.call(
            i64(car.time),
            f64(car.location.x),
            f64(car.location.y),
            f64(car.velocity.angle.radians),
            f64(car.velocity.magnitude),
            i32(car.lap),
            i32(car.nextCheckpoint),
        )
    }

    override fun onDraw(): List<DrawRequest> {
        onDraw?.callVoid()
        return drawRequests.toList().also {
            drawRequests.clear()
        }
    }

    override fun close() {
        guest.close()
    }
}

private class WasmGuest : AutoCloseable {
    lateinit var store: Store
    lateinit var linker: Linker<*>
    lateinit var instance: Instance
    lateinit var memory: WasmMemory

    override fun close() {
        instance.close()
        linker.close()
        store.close()
    }
}

private fun WasmMemory.require(byteCount: Int) {
    val pages = byteCount / pageSize() + (byteCount % pageSize()).coerceAtMost(1)
    if (size < pages) {
        if (grow(pages - size) == -1) {
            error("unable to allocate sufficient memory: $byteCount")
        }
    }
}

private fun fdWrite(
    memory: WasmMemory,
    iovs: Int,
    iovs_len: Int,
    fd: Int,
    nwritten: Int,
    stdout: Sink,
    stderr: Sink,
): Int {
    var bytesWritten = 0

    val sink = when (fd) {
        1 -> stdout
        2 -> stderr
        else -> {
            memory.writeInt32(nwritten.toLong(), 0)
            return 1
        }
    }

    for (i in 0L..<iovs_len) {
        val startIndex = memory.readInt32(iovs + i * 8L)
        val byteCount = memory.readInt32(iovs + i * 8L + 4L)
        sink.write(memory, startIndex, byteCount)
        bytesWritten += byteCount
    }

    memory.writeInt32(nwritten.toLong(), bytesWritten)
    return 0
}

private fun randomGet(
    memory: WasmMemory,
    random: Random,
    bufLen: Int,
    bufPtr: Int
): Int {
    val randomBytes = random.nextBytes(bufLen)
    memory.writeBytes(bufPtr, randomBytes, 0, randomBytes.size)
    return 0
}

@OptIn(UnsafeIoApi::class, DelicateIoApi::class)
private fun Sink.write(
    memory: WasmMemory,
    startIndex: Int,
    byteCount: Int
) {
    var remaining = byteCount
    var offset = startIndex

    writeToInternalBuffer { buffer ->
        while (remaining > 0) {
            val length = minOf(remaining, 8192)
            UnsafeBufferOperations.writeToTail(buffer, length) { dest, destOffset, _ ->
                memory.readBytes(offset, dest, destOffset, length)
                length
            }

            remaining -= length
            offset += length
        }
    }
}
