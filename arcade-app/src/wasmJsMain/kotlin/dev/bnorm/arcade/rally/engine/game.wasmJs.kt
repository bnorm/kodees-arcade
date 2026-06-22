@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.bnorm.arcade.rally.engine

import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.rally.Car
import dev.bnorm.arcade.rally.Point
import dev.bnorm.arcade.rally.Track
import dev.bnorm.arcade.rally.Velocity
import io.github.vinceglb.filekit.core.PlatformFile
import js.array.Tuple
import js.buffer.ArrayBuffer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import web.assembly.*
import web.encoding.TextDecoder
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
actual fun CoroutineScope.game(
    track: Track,
    paths: Map<String, PlatformFile>,
    carImages: List<ImageBitmap>
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

    send(gameState)

    val racers = paths.map { (name, path) ->
        try {
            val racerState = gameState.racers.getValue(name)
            val racer = WasmRacer.create(racerState, path.readBytes(), name)
            racer to racerState
        } catch (t: Throwable) {
            t.printStackTrace()
            throw t
        }
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

@OptIn(ExperimentalSerializationApi::class)
private class WasmRacer(
    val name: String,
    val module: Module,
    val instance: Instance,
    private val memory: Memory<ArrayBuffer>,
    private val moveFunction: JsFunction<Tuple, JsAny?>,
    private val onRaceFunction: JsFunction<Tuple, JsAny?>,
) : Racer {

    companion object {
        suspend fun create(
            racerState: RallyRacerState,
            racer: ByteArray,
            name: String,
        ): WasmRacer {
            lateinit var memory: Memory<ArrayBuffer>

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

            val racer = WasmRacer(
                name = name,
                module = module,
                instance = instance,
                memory = memory,
                moveFunction = instance.exports["move"]!!.unsafeCast(),
                onRaceFunction = instance.exports["onRace"]!!.unsafeCast(),
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
        val view = DataView(memory.buffer, byteOffset = 0)
        view.setInt32(0, bytes.size, true)
        repeat(bytes.size) {
            view.setInt8(4 + it, bytes[it])
        }

        invoke(moveFunction)
    }

    fun onRace(track: Track) {
        val bytes = ProtoBuf.encodeToByteArray(Track.serializer(), track)

        memory.ensureBytes(4 + bytes.size)
        val view = DataView(memory.buffer, byteOffset = 0)
        view.setInt32(0, bytes.size, true)
        repeat(bytes.size) {
            view.setInt8(4 + it, bytes[it])
        }

        invoke(onRaceFunction)
    }

    override fun close() {
    }
}

private fun Memory<*>.ensureBytes(pages: Int) {
    if (buffer.byteLength < pages) {
        grow(pages - buffer.byteLength)
    }
}

//private fun JsFunction(func: () -> Unit): JsFunction<Tuple, JsAny?> = js("func")

private fun invoke(func: JsFunction<Tuple, JsAny?>) {
    js("func()")
}

// TODO convert this to Kotlin, somehow...
// TODO use https://github.com/easywasm/wasi instead of custom 'wasi_snapshot_preview1'?
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
    memory: Memory<ArrayBuffer>,
    iovs_len: Int,
    iovs: Int,
    fd: Int,
    nwritten: Int
): Int {
    val view = DataView(memory.buffer)
    var bytesWritten = 0

    // Iterate over the iovecs (scatter/gather arrays)
    for (i in 0..<iovs_len) {
        val iov_base = view.getUint32(iovs + i * 8, true)
        val iov_len = view.getUint32(iovs + i * 8 + 4, true)

        // Extract the bytes from linear memory
        val buffer = Uint8Array(memory.buffer, iov_base, iov_len)

        // Write or append to output
        if (fd == 1) { // STDOUT
            println("[$name] " + TextDecoder("utf-8").decode(buffer))
        } else if (fd == 2) { // STDERR
            @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
            printError("[$name] " + TextDecoder("utf-8").decode(buffer))
        }

        bytesWritten += iov_len
    }

    // Write the number of bytes written back to Wasm memory
    view.setUint32(nwritten, bytesWritten, true)

    return 0 // Return 0 (WASI success)
}

private fun randomGet(
    memory: Memory<ArrayBuffer>,
    bufLen: Int,
    bufPtr: Int
): Int {
    val memory = Uint8Array(memory.buffer)
    val randomBytes = Random.nextBytes(bufLen).toUint8Array()
    memory.set(randomBytes, bufPtr)
    return 0
}
