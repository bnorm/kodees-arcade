@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.bnorm.arcade.rally.engine.wasm

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
import js.array.Tuple
import js.buffer.DataView
import js.buffer.toArrayBuffer
import js.function.JsFunction
import js.numbers.JsDouble
import js.numbers.JsFloat
import js.numbers.JsInt
import js.numbers.JsNumbers.toJsDouble
import js.numbers.JsNumbers.toJsInt
import js.numbers.JsNumbers.toKotlinFloat
import js.objects.get
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import js.typedarrays.toUint8Array
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import web.assembly.Imports
import web.assembly.Instance
import web.assembly.Memory
import web.assembly.Module
import web.assembly.compile
import web.assembly.instantiate

actual suspend fun WasmDriver(name: String, bytes: ByteArray): WasmDriver {
    return BrowserWasmDriver.of(name, bytes)
}

private class BrowserWasmDriver : WasmDriver {
    companion object {
        suspend fun of(name: String, bytes: ByteArray): WasmDriver {
            val driver = BrowserWasmDriver()
            val guest = driver.guest

            guest.imports = Imports(
                getThrottle = {
                    driver.throttle.toJsDouble()
                },
                setThrottle = { throttle ->
                    driver.throttle = throttle.toDouble()
                },
                getSteering = {
                    driver.steering.toJsDouble()
                },
                setSteering = { steering ->
                    driver.steering = steering.toDouble()
                },
                playerCanvasDraw = { p0, p1, p2, p3, p4, p5, p6, p7, p8 ->
                    driver.drawRequests.add(
                        readDrawRequest(
                            p0.toInt(),
                            p1.toInt(),
                            p2.toDouble(),
                            p3.toDouble(),
                            p4.toDouble(),
                            p5.toDouble(),
                            p6.toLong(),
                            p7.toInt(),
                            p8.toKotlinFloat()
                        )
                    )
                },
                fdWrite = { fd, iovs, iovs_len, nwritten ->
                    fdWrite(
                        guest.memory,
                        iovs_len.toInt(),
                        iovs.toInt(),
                        fd.toInt(),
                        nwritten.toInt(),
                        driver.stdout,
                        driver.stderr,
                    ).toJsInt()
                },
                randomGet = { bufPtr, bufLen ->
                    randomGet(guest.memory, driver.random, bufLen.toInt(), bufPtr.toInt()).toJsInt()
                },
                environSizesGet = { environCountPtr, environBufferSizePtr ->
                    // TODO what environment variables should we expose?
                    val view = DataView(guest.memory.buffer)
                    view.setInt32(environCountPtr.toInt(), 0)
                    view.setInt32(environBufferSizePtr.toInt(), 0)
                    0.toJsInt()
                },
                environGet = { environPtr, environBufferPtr ->
                    // TODO what environment variables should we expose?
                    0.toJsInt()
                },
                procExit = { errorCode ->
                    // TODO how to exit?
                },
            )

            guest.module = compile(bytes.toArrayBuffer())
            guest.instance = instantiate(guest.module, guest.imports)
            guest.memory = guest.instance.exports["memory"]!!.unsafeCast()
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

    private val move: JsFunction<Tuple, JsAny?> by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.exports["bnorm:arcade/driver#on-turn"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
    }

    private val onRace by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.exports["bnorm:arcade/driver#on-race"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
    }

    private val onDraw by lazy(LazyThreadSafetyMode.NONE) {
        guest.instance.exports["bnorm:arcade/driver#on-draw"]?.unsafeCast<JsFunction<Tuple, JsAny?>>()
    }

    override fun move(car: Car) {
        move.invoke(
            car.time,
            car.location.x,
            car.location.y,
            car.velocity.angle.radians,
            car.velocity.magnitude,
            car.lap,
            car.nextCheckpoint,
        )
    }

    override fun onRace(race: Race) {
        guest.memory.require(
            race.track.checkpoints.size * 32 +
                race.track.positions.size * 24
        )

        val view = DataView(guest.memory.buffer, byteOffset = 0)

        val checkpointsPtr = 0
        for ((i, checkpoint) in race.track.checkpoints.withIndex()) {
            val base = (checkpointsPtr) + (i * 32)
            view.setFloat64(base + 0, checkpoint.start.x, littleEndian = true)
            view.setFloat64(base + 8, checkpoint.start.y, littleEndian = true)
            view.setFloat64(base + 16, checkpoint.end.x, littleEndian = true)
            view.setFloat64(base + 24, checkpoint.end.y, littleEndian = true)
        }

        val positionsPtr = race.track.checkpoints.size * 32
        for ((i, position) in race.track.positions.withIndex()) {
            val base = (positionsPtr) + (i * 24)
            view.setFloat64(base + 0, position.location.x, littleEndian = true)
            view.setFloat64(base + 8, position.location.y, littleEndian = true)
            view.setFloat64(base + 16, position.heading.radians, littleEndian = true)
        }

        onRace.invoke(
            race.track.width,
            race.track.height,
            checkpointsPtr,
            race.track.checkpoints.size,
            positionsPtr,
            race.track.positions.size,
            race.laps,
        )
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
    lateinit var imports: Imports
    lateinit var instance: Instance
    lateinit var memory: Memory<*>

    override fun close() {}
}

private fun JsFunction<Tuple, JsAny?>.invoke() {
    invoke(this)
}

private fun JsFunction<Tuple, JsAny?>.invoke(
    p0: Double,
    p1: Double,
    p2: Int,
    p3: Int,
    p4: Int,
    p5: Int,
    p6: Int,
) {
    invoke(this, p0.toJsDouble(), p1.toJsDouble(), p2.toJsInt(), p3.toJsInt(), p4.toJsInt(), p5.toJsInt(), p6.toJsInt())
}

private fun JsFunction<Tuple, JsAny?>.invoke(
    p0: Long,
    p1: Double,
    p2: Double,
    p3: Double,
    p4: Double,
    p5: Int,
    p6: Int,
) {
    invoke(
        this,
        p0.toJsBigInt(),
        p1.toJsDouble(),
        p2.toJsDouble(),
        p3.toJsDouble(),
        p4.toJsDouble(),
        p5.toJsInt(),
        p6.toJsInt()
    )
}

@Suppress("unused")
private fun invoke(func: JsFunction<Tuple, JsAny?>) {
    js("func()")
}

@Suppress("unused")
private fun invoke(
    func: JsFunction<Tuple, JsAny?>,
    p0: JsAny,
    p1: JsAny,
    p2: JsAny,
    p3: JsAny,
    p4: JsAny,
    p5: JsAny,
    p6: JsAny,
) {
    js("func(p0, p1, p2, p3, p4, p5, p6)")
}

// TODO convert this to Kotlin, somehow...
@Suppress("unused")
private fun Imports(
    getThrottle: () -> JsDouble,
    setThrottle: (JsDouble) -> Unit,
    getSteering: () -> JsDouble,
    setSteering: (JsDouble) -> Unit,
    playerCanvasDraw: (JsInt, JsInt, JsDouble, JsDouble, JsDouble, JsDouble, JsBigInt, JsInt, JsFloat) -> Unit,
    fdWrite: (JsInt, JsInt, JsInt, JsInt) -> JsInt,
    randomGet: (JsInt, JsInt) -> JsInt,
    environSizesGet: (JsInt, JsInt) -> JsInt,
    environGet: (JsInt, JsInt) -> JsInt,
    procExit: (JsInt) -> Unit,
): Imports = js(
    """
        ({
            'bnorm:arcade/controls': {
               'throttle-get': getThrottle,
               'throttle-set': setThrottle,
               'steering-get': getSteering,
               'steering-set': setSteering
            },
            'bnorm:arcade/canvas': {
                draw: playerCanvasDraw
            },
            wasi_snapshot_preview1: {
                fd_write: fdWrite,
                random_get: randomGet,
                environ_sizes_get: environSizesGet,
                environ_get: environGet,
                proc_exit: procExit
            }
        })
    """
)

private fun Memory<*>.require(byteCount: Int) {
    val pages = byteCount
    if (buffer.byteLength < pages) {
        if (grow(pages - buffer.byteLength) == -1) {
            error("unable to allocate sufficient memory: $byteCount")
        }
    }
}

private fun readDrawRequest(
    p0: Int,
    p1: Int,
    p2: Double,
    p3: Double,
    p4: Double,
    p5: Double,
    p6: Long,
    p7: Int,
    p8: Float,
): DrawRequest {
    return when (p0) {
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
}

private fun fdWrite(
    memory: Memory<*>,
    iovs_len: Int,
    iovs: Int,
    fd: Int,
    nwritten: Int,
    stdout: Sink,
    stderr: Sink,
): Int {
    val view = DataView(memory.buffer)
    var bytesWritten = 0

    for (i in 0..<iovs_len) {
        val iov_base = view.getUint32(iovs + i * 8, true)
        val iov_len = view.getUint32(iovs + i * 8 + 4, true)

        val buffer = Uint8Array(memory.buffer, iov_base, iov_len)

        when (fd) {
            1 -> stdout.write(buffer.toByteArray())
            2 -> stderr.write(buffer.toByteArray())
        }

        bytesWritten += iov_len
    }

    view.setUint32(nwritten, bytesWritten, true)
    return 0
}

private fun randomGet(
    memory: Memory<*>,
    random: Random,
    bufLen: Int,
    bufPtr: Int
): Int {
    val memory = Uint8Array(memory.buffer)
    val randomBytes = random.nextBytes(bufLen).toUint8Array()
    memory.set(randomBytes, bufPtr)
    return 0
}
