@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.bnorm.arcade.rally.engine.wasm

import dev.bnorm.arcade.rally.engine.RallyRacerState
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
import js.typedarrays.toUint8Array
import web.assembly.Imports
import web.assembly.Memory
import web.assembly.compile
import web.assembly.instantiate
import web.encoding.TextDecoder
import kotlin.random.Random

actual suspend fun WasmEngine.createWasmRacer(
    racerState: RallyRacerState,
    racer: ByteArray,
    name: String
): WasmRacer {
    lateinit var memory: Memory<*>

    val imports = Imports(
        getThrottle = { racerState.throttle.toJsDouble() },
        setThrottle = { throttle ->
            racerState.throttle = throttle.toDouble()
        },
        getSteering = { racerState.steering.toJsDouble() },
        setSteering = { steering ->
            racerState.steering = steering.toDouble()
        },
        fdWrite = { fd, iovs, iovs_len, nwritten ->
            fdWrite(name, memory, iovs_len.toInt(), iovs.toInt(), fd.toInt(), nwritten.toInt()).toJsInt()
        },
        randomGet = { bufPtr, bufLen ->
            randomGet(memory, bufLen.toInt(), bufPtr.toInt()).toJsInt()
        }
    )

    val module = compile(racer.toArrayBuffer())
    val instance = instantiate(module, imports)

    memory = instance.exports["memory"]!!.unsafeCast()

    val moveFunction = instance.exports["move"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
    val onRaceFunction = instance.exports["onRace"]!!.unsafeCast<JsFunction<Tuple, JsAny?>>()
    val racer = WasmRacer(
        memory = BrowserMemory(memory),
        moveFunction = { invoke(moveFunction) },
        onRaceFunction = { invoke(onRaceFunction) },
    )

    return racer
}

@Suppress("unused")
private fun invoke(func: JsFunction<Tuple, JsAny?>) {
    js("func()")
}

// TODO convert this to Kotlin, somehow...
// TODO use https://github.com/easywasm/wasi instead of custom 'wasi_snapshot_preview1'?
@Suppress("unused")
private fun Imports(
    getThrottle: () -> JsDouble,
    setThrottle: (JsDouble) -> Unit,
    getSteering: () -> JsDouble,
    setSteering: (JsDouble) -> Unit,
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
            wasi_snapshot_preview1: {
                fd_write: fdWrite,
                random_get: randomGet
            }
        })
    """
)

private fun fdWrite(
    name: String,
    memory: Memory<*>,
    iovs_len: Int,
    iovs: Int,
    fd: Int,
    nwritten: Int
): Int {
    val view = DataView(memory.buffer)
    var bytesWritten = 0

    for (i in 0..<iovs_len) {
        val iov_base = view.getUint32(iovs + i * 8, true)
        val iov_len = view.getUint32(iovs + i * 8 + 4, true)

        val buffer = Uint8Array(memory.buffer, iov_base, iov_len)

        if (fd == 1) {
            println("[$name] " + TextDecoder("utf-8").decode(buffer))
        } else if (fd == 2) {
            @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
            printError("[$name] " + TextDecoder("utf-8").decode(buffer))
        }

        bytesWritten += iov_len
    }

    view.setUint32(nwritten, bytesWritten, true)
    return 0
}

private fun randomGet(
    memory: Memory<*>,
    bufLen: Int,
    bufPtr: Int
): Int {
    val memory = Uint8Array(memory.buffer)
    val randomBytes = Random.nextBytes(bufLen).toUint8Array()
    memory.set(randomBytes, bufPtr)
    return 0
}
