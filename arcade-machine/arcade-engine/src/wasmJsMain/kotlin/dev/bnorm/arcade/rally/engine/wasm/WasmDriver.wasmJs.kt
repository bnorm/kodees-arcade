@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.driver.Car
import dev.bnorm.arcade.driver.Race
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import js.array.Tuple
import js.buffer.DataView
import js.buffer.toArrayBuffer
import js.function.JsFunction
import js.numbers.JsDouble
import js.numbers.JsInt
import js.numbers.JsNumbers.toJsDouble
import js.numbers.JsNumbers.toJsInt
import js.objects.get
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import js.typedarrays.toUint8Array
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
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
                playerCanvasDraw = { offset ->
                    driver.drawRequests.add(readDrawRequest(guest.memory, offset.toInt()))
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
                }
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

    private val move by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.exports["move"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
        function::invoke
    }

    private val onRace by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.exports["onRace"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
        function::invoke
    }

    private val onDraw by lazy(LazyThreadSafetyMode.NONE) {
        val function = guest.instance.exports["onDraw"]?.unsafeCast<JsFunction<Tuple, JsAny?>>()
        if (function == null) null else function::invoke
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
    lateinit var imports: Imports
    lateinit var instance: Instance
    lateinit var memory: Memory<*>

    override fun close() {}
}

private fun JsFunction<Tuple, JsAny?>.invoke() {
    invoke(this)
}

@Suppress("unused")
private fun invoke(func: JsFunction<Tuple, JsAny?>) {
    js("func()")
}

// TODO convert this to Kotlin, somehow...
@Suppress("unused")
private fun Imports(
    getThrottle: () -> JsDouble,
    setThrottle: (JsDouble) -> Unit,
    getSteering: () -> JsDouble,
    setSteering: (JsDouble) -> Unit,
    playerCanvasDraw: (JsInt) -> Unit,
    fdWrite: (JsInt, JsInt, JsInt, JsInt) -> JsInt,
    randomGet: (JsInt, JsInt) -> JsInt
): Imports = js(
    """
        ({
            rally_api: {
               controls_throttle_get: getThrottle,
               controls_throttle_set: setThrottle,
               controls_steering_get: getSteering,
               controls_steering_set: setSteering
            },
            player_canvas: {
                draw: playerCanvasDraw
            },
            wasi_snapshot_preview1: {
                fd_write: fdWrite,
                random_get: randomGet
            }
        })
    """
)

private fun <T> Memory<*>.writeProto(offset: Int, serializer: KSerializer<T>, value: T) {
    val bytes = ProtoBuf.encodeToByteArray(serializer, value)

    val pages = offset + 4 + bytes.size
    if (buffer.byteLength < pages) {
        grow(pages - buffer.byteLength)
    }

    val view = DataView(buffer, byteOffset = offset)
    view.setInt32(0, bytes.size, littleEndian = true)
    repeat(bytes.size) {
        view.setInt8(4 + it, bytes[it])
    }
}

private fun readDrawRequest(
    memory: Memory<*>,
    offset: Int
): DrawRequest {
    val size = DataView(memory.buffer).getUint32(offset, true)
    val bytes = Uint8Array(memory.buffer, offset + 4, size).toByteArray()
    return ProtoBuf.decodeFromByteArray(DrawRequest.serializer(), bytes)
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
