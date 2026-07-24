@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import js.array.Tuple
import js.buffer.toArrayBuffer
import js.function.JsFunction
import js.numbers.JsDouble
import js.numbers.JsFloat
import js.numbers.JsInt
import js.numbers.JsNumbers.toJsDouble
import js.numbers.JsNumbers.toJsInt
import js.numbers.JsNumbers.toKotlinFloat
import js.objects.get
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import web.assembly.Imports
import web.assembly.Instance
import web.assembly.Module
import web.assembly.compile
import web.assembly.instantiate

actual suspend fun WasmModule(bytes: ByteArray): WasmModule {
    return BrowserWasmModule(compile(bytes.toArrayBuffer()))
}

private class BrowserWasmModule(
    private val module: Module
) : WasmModule {
    override suspend fun createDriver(name: String): WasmDriver {
        return BrowserWasmDriver.of(name, module)
    }
}

private class BrowserWasmDriver private constructor(
    override val name: String,
) : WasmDriver {
    companion object {
        suspend fun of(name: String, module: Module): WasmDriver {
            val driver = BrowserWasmDriver(name)

            val imports = Imports(
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
                        memory = driver.memory,
                        iovs = iovs.toInt(),
                        iovs_len = iovs_len.toInt(),
                        fd = fd.toInt(),
                        nwritten = nwritten.toInt(),
                        stdout = driver.stdout,
                        stderr = driver.stderr,
                    ).toJsInt()
                },
                randomGet = { bufPtr, bufLen ->
                    randomGet(driver.memory, driver.random, bufLen.toInt(), bufPtr.toInt()).toJsInt()
                },
                environSizesGet = { environCountPtr, environBufferSizePtr ->
                    // TODO what environment variables should we expose?
                    driver.memory.writeInt32(environCountPtr.toInt(), 0)
                    driver.memory.writeInt32(environBufferSizePtr.toInt(), 0)
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

            driver.instance = instantiate(module, imports)
            driver.memory = BrowserWasmInstanceMemory(driver.instance.exports["memory"]!!.unsafeCast())
            return driver
        }
    }

    lateinit var instance: Instance
    lateinit var memory: WasmInstanceMemory

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

    private val onRace: JsFunction<Tuple, JsAny?> by lazy(LazyThreadSafetyMode.NONE) {
        instance.exports["bnorm:arcade/driver#on-race"]!!.unsafeCast()
    }

    private val onTurn: JsFunction<Tuple, JsAny?> by lazy(LazyThreadSafetyMode.NONE) {
        instance.exports["bnorm:arcade/driver#on-turn"]!!.unsafeCast()
    }

    private val onDraw: JsFunction<Tuple, JsAny?>? by lazy(LazyThreadSafetyMode.NONE) {
        instance.exports["bnorm:arcade/driver#on-draw"]?.unsafeCast()
    }

    override fun onRace(race: Race) {
        memory.require(
            race.track.checkpoints.size * 32 +
                race.track.positions.size * 24
        )

        val checkpointsPtr = 0
        for ((i, checkpoint) in race.track.checkpoints.withIndex()) {
            val base = (checkpointsPtr) + (i * 32)
            memory.writeFloat64(base + 0, checkpoint.start.x)
            memory.writeFloat64(base + 8, checkpoint.start.y)
            memory.writeFloat64(base + 16, checkpoint.end.x)
            memory.writeFloat64(base + 24, checkpoint.end.y)
        }

        val positionsPtr = race.track.checkpoints.size * 32
        for ((i, position) in race.track.positions.withIndex()) {
            val base = (positionsPtr) + (i * 24)
            memory.writeFloat64(base + 0, position.location.x)
            memory.writeFloat64(base + 8, position.location.y)
            memory.writeFloat64(base + 16, position.heading.radians)
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

    override fun onTurn(car: Car) {
        onTurn.invoke(
            car.time,
            car.location.x,
            car.location.y,
            car.velocity.angle.radians,
            car.velocity.magnitude,
            car.lap,
            car.nextCheckpoint,
        )
    }

    override fun onDraw(): List<DrawRequest> {
        onDraw?.invoke()
        return drawRequests.toList().also {
            drawRequests.clear()
        }
    }

    override fun close() {
    }
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
