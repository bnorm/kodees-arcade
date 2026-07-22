package dev.bnorm.arcade.rally.engine.wasm

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
import dev.bnorm.arcade.driver.canvas.Color
import dev.bnorm.arcade.driver.canvas.Fill
import dev.bnorm.arcade.driver.canvas.Stroke
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Circle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Rectangle
import dev.bnorm.arcade.geometry.Segment
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
                    "bnorm:arcade/controls", "throttle-set",
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
                        val p0 = params[0].asInt()
                        val p1 = params[1].asInt()
                        val p2 = params[2].asDouble()
                        val p3 = params[3].asDouble()
                        val p4 = params[4].asDouble()
                        val p5 = params[5].asDouble()
                        val p6 = params[6].asLong()
                        val p7 = params[7].asInt()
                        val p8 = params[8].asFloat()

                        val request = when (p0) {
                            0 -> DrawRequest.Segment(
                                color = Color(p1.toUInt()),
                                segment = Segment(
                                    start = Point(
                                        x = p2,
                                        y = p3,
                                    ),
                                    end = Point(
                                        x = p4,
                                        y = p5,
                                    )
                                ),
                                stroke = Stroke(Float.fromBits(p6.toInt())),
                            )

                            1 -> DrawRequest.Circle(
                                color = Color(p1.toUInt()),
                                circle = Circle(
                                    center = Point(
                                        x = p2,
                                        y = p3,
                                    ),
                                    radius = p4,
                                ),
                                startAngle = Angle.ofRadians(p5),
                                sweepAngle = Angle.ofRadians(Double.fromBits(p6)),
                                style = when (p7) {
                                    0 -> Fill
                                    1 -> Stroke(
                                        width = p8,
                                    )

                                    else -> error("!")
                                }
                            )

                            2 -> DrawRequest.Rectangle(
                                color = Color(p1.toUInt()),
                                rectangle = run {
                                    val minX = p2
                                    val maxX = p3
                                    val minY = p4
                                    val maxY = p5
                                    Rectangle(
                                        center = Point((minX + maxX) / 2.0, (minY + maxY) / 2.0),
                                        width = maxX - minX,
                                        height = maxY - minY,
                                    )
                                },
                                style = when (p6) {
                                    0L -> Fill
                                    1L -> Stroke(
                                        width = Float.fromBits(p7),
                                    )

                                    else -> error("!")
                                }
                            )

                            else -> error("!")
                        }
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
        guest.instance.getFunction("bnorm:arcade/driver#on-turn").getOrNull()!!
    }

    private val onRace by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.getFunction("bnorm:arcade/driver#on-race").getOrNull()!!
    }

    private val onDraw by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.getFunction("bnorm:arcade/driver#on-draw").getOrNull()
    }

    override fun move(car: Car) {
        move.call(
            i64(car.time),
            f64(car.location.x),
            f64(car.location.y),
            f64(car.velocity.angle.radians),
            f64(car.velocity.magnitude),
            i32(car.lap),
            i32(car.nextCheckpoint),
        )
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
