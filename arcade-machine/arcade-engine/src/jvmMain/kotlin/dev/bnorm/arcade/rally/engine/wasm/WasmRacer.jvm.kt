package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.WasmValue
import ai.tegmentum.wasmtime4j.WasmValueType
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import dev.bnorm.arcade.driver.canvas.internal.DrawRequest
import dev.bnorm.arcade.rally.engine.DriverControlState
import kotlin.jvm.optionals.getOrNull

actual suspend fun WasmEngine.createWasmDriver(
    controlState: DriverControlState,
    driver: ByteArray,
    name: String,
): WasmDriver {
    val drawRequests = mutableListOf<DrawRequest>()

    val module = compileModule(driver)
    val store = createStore()

    lateinit var memory: Wasmtime4jMemory
    val linker = createLinker(this, { memory }, controlState, drawRequests)
    val instance = linker.instantiate(store, module)

    memory = Wasmtime4jMemory(instance.defaultMemory.orElseThrow())
    val moveFunction = instance.getFunction("move").orElseThrow()
    val onRaceFunction = instance.getFunction("onRace").orElseThrow()
    val onDrawFunction = instance.getFunction("onDraw").getOrNull()
    return WasmDriver(
        memory = memory,
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
    engine: Engine,
    memory: () -> Wasmtime4jMemory,
    controlState: DriverControlState,
    drawBuffer: MutableList<DrawRequest>,
): Linker<*> {
    val runtime = engine.runtime
    // TODO need to redirect IO into buffer of some kind
    val context = runtime.createWasiContext().inheritStdio()
    val linker = runtime.createLinker<WasiContext?>(engine)
    runtime.addWasiToLinker(linker, context)

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
            val request = memory().readProto(offset.asInt(), DrawRequest.serializer())
            drawBuffer.add(request)
        },
    )

    return linker
}
