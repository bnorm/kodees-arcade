package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Instance
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.Module
import ai.tegmentum.wasmtime4j.Store
import ai.tegmentum.wasmtime4j.WasmMemory
import ai.tegmentum.wasmtime4j.WasmValue.f64
import ai.tegmentum.wasmtime4j.WasmValue.i32
import ai.tegmentum.wasmtime4j.WasmValueType.F64
import ai.tegmentum.wasmtime4j.WasmValueType.I32
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

actual suspend fun WasmDriver(name: String, bytes: ByteArray): WasmDriver {
    return WasmtimeWasmDriver.of(bytes)
}

private class WasmtimeWasmDriver private constructor() : WasmDriver {
    companion object {
        fun of(bytes: ByteArray): WasmtimeWasmDriver {
            val driver = WasmtimeWasmDriver()
            val guest = driver.guest

            // TODO can we cache these?
            //  - this is probably the longest process of creating a driver
            //  - maybe it should come pre-compiled, so we know it's a valid wasm program?
            //  - caching might be a lot of memory pressure...
            //  - but it could help with running the same driver multiple times
            guest.module = engine.compileModule(bytes)

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
                    "rally_api", "controls_throttle_get",
                    FunctionType(arrayOf(), arrayOf(F64)),
                    singleValue { f64(driver.throttle) },
                )
                defineHostFunction(
                    "rally_api", "controls_throttle_set",
                    FunctionType(arrayOf(F64), arrayOf()),
                    voidFunction { (throttle) -> driver.throttle = throttle.asDouble() },
                )
                defineHostFunction(
                    "rally_api", "controls_steering_get",
                    FunctionType(arrayOf(), arrayOf(F64)),
                    singleValue { f64(driver.steering) },
                )
                defineHostFunction(
                    "rally_api", "controls_steering_set",
                    FunctionType(arrayOf(F64), arrayOf()),
                    voidFunction { (steering) -> driver.steering = steering.asDouble() },
                )
                defineHostFunction(
                    "player_canvas", "draw",
                    FunctionType(arrayOf(I32), arrayOf()),
                    voidFunction { (offset) ->
                        val request = guest.memory.readProto(offset.asInt(), DrawRequest.serializer())
                        driver.drawRequests.add(request)
                    },
                )
            }

            guest.instance = guest.linker.instantiate(guest.store, guest.module)
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

    private val move by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.getFunction("move").getOrNull()!!
        function::callVoid
    }

    private val onRace by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.getFunction("onRace").getOrNull()!!
        function::callVoid
    }

    private val onDraw by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.getFunction("onDraw").getOrNull()
        if (function == null) null else function::callVoid
    }

    override fun move(car: Car) {
        guest.memory.writeProto(0, Car.serializer(), car)
        move.invoke()
    }

    override fun onRace(race: Race) {
        guest.memory.writeProto(0, Race.serializer(), race)
        onRace.invoke()
    }

    override fun onDraw(): List<DrawRequest> {
        onDraw?.invoke()
        return drawRequests.toList().also {
            drawRequests.clear()
        }
    }

    override fun close() {
        guest.close()
    }
}

private class WasmGuest : AutoCloseable {
    lateinit var module: Module
    lateinit var store: Store
    lateinit var linker: Linker<*>
    lateinit var instance: Instance
    lateinit var memory: WasmMemory

    override fun close() {
        instance.close()
        linker.close()
        store.close()
        module.close()
    }
}

private fun <T> WasmMemory.writeProto(offset: Int, serializer: KSerializer<T>, value: T) {
    val bytes = ProtoBuf.encodeToByteArray(serializer, value)

    val byteCount = offset + 4 + bytes.size
    val pages = byteCount / pageSize() + (byteCount % pageSize()).coerceAtMost(1)
    if (size < pages) {
        grow(pages - size)
    }

    writeInt32(offset.toLong(), bytes.size)
    writeBytes(offset + 4, bytes, 0, bytes.size)
}

private fun <T> WasmMemory.readProto(offset: Int, serializer: KSerializer<T>): T {
    val byteCount = readInt32(offset.toLong())
    val bytes = ByteArray(byteCount)
    readBytes(offset + 4, bytes, 0, byteCount)
    return ProtoBuf.decodeFromByteArray(serializer, bytes)
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
