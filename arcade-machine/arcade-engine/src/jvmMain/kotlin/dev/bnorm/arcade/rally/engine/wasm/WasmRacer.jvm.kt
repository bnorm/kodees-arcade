package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.WasmMemory
import ai.tegmentum.wasmtime4j.WasmValue
import ai.tegmentum.wasmtime4j.WasmValueType
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.rally.engine.DriverControlState
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

actual suspend fun WasmEngine.createWasmDriver(
    controlState: DriverControlState,
    driver: ByteArray,
    name: String,
): WasmDriver {
    val drawRequests = mutableListOf<DrawRequest>()
    lateinit var delegate: WasmMemory

    val module = compileModule(driver)
    val store = createStore()

    val linker = createLinker(name, this, { delegate }, controlState, drawRequests)
    val instance = linker.instantiate(store, module)

    delegate = instance.defaultMemory.orElseThrow()
    val moveFunction = instance.getFunction("move").orElseThrow()
    val onRaceFunction = instance.getFunction("onRace").orElseThrow()
    val onDrawFunction = instance.getFunction("onDraw").getOrNull()
    return WasmDriver(
        memory = Wasmtime4jMemory(delegate),
        moveFunction = { moveFunction.callVoid() },
        onRaceFunction = { onRaceFunction.callVoid() },
        onDrawFunction = onDrawFunction?.let { { onDrawFunction.callVoid() } },
        onClose = {
            instance.close()
            store.close()
            module.close()
            linker.close()
        },
        drawRequests = drawRequests,
    )
}

private fun createLinker(
    name: String,
    engine: Engine,
    memory: () -> WasmMemory,
    controlState: DriverControlState,
    drawBuffer: MutableList<DrawRequest>,
): Linker<*> {
    val runtime = engine.runtime
    val linker = runtime.createLinker<Nothing>(engine)

    linker.defineHostFunction(
        "wasi_snapshot_preview1",
        "fd_write",
        FunctionType(
            arrayOf(WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32),
            arrayOf(WasmValueType.I32)
        ),
        HostFunction.singleValue { (iovs_len, iovs, fd, nwritten) ->
            val memory = memory()
            var bytesWritten = 0

            val iovs = iovs.asLong()

            for (i in 0..<iovs_len.asInt()) {
                val iov_base = memory.readInt32(iovs + i * 8)
                val iov_len = memory.readInt32(iovs + i * 8 + 4)

                val buffer = ByteArray(iov_len)
                memory.readBytes(iov_base, buffer, 0, buffer.size)

                if (fd.asInt() == 1) {
                    println("[$name] " + buffer.decodeToString())
                } else if (fd.asInt() == 2) {
                    System.err.println("[$name] " + buffer.decodeToString())
                }

                bytesWritten += iov_len
            }

            memory.writeInt32(nwritten.asLong(), bytesWritten)
            WasmValue.i32(0)
        },
    )

    linker.defineHostFunction(
        "wasi_snapshot_preview1",
        "random_get",
        FunctionType(
            arrayOf(WasmValueType.I32, WasmValueType.I32),
            arrayOf(WasmValueType.I32)
        ),
        HostFunction.singleValue { (bufLen, bufPtr) ->
            val memory = memory()
            val randomBytes = Random.nextBytes(bufLen.asInt())
            memory.writeBytes(bufPtr.asInt(), randomBytes, 0, randomBytes.size)
            WasmValue.i32(0)
        },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(controlState.throttle) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (throttle) -> controlState.throttle = throttle.asDouble() },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(controlState.steering) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (steering) -> controlState.steering = steering.asDouble() },
    )

    linker.defineHostFunction(
        "player_canvas",
        "draw",
        FunctionType(arrayOf(WasmValueType.I32), arrayOf()),
        HostFunction.voidFunction { (offset) ->
            val request = Wasmtime4jMemory(memory()).readProto(offset.asInt(), DrawRequest.serializer())
            drawBuffer.add(request)
        },
    )

    return linker
}
