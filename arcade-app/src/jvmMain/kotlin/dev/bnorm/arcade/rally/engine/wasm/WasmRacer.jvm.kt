package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.WasmValue
import ai.tegmentum.wasmtime4j.WasmValueType
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils
import dev.bnorm.arcade.rally.engine.RallyRacerState

actual suspend fun WasmEngine.createWasmRacer(
    racerState: RallyRacerState,
    racer: ByteArray,
    name: String
): WasmRacer {
    val linker = createRacerLinker(this, racerState)
    val module = compileModule(racer)
    val store = createStore()
    val instance = linker.instantiate(store, module)

    val memory = Wasmtime4jMemory(instance.defaultMemory.orElseThrow())
    val moveFunction = instance.getFunction("move").orElseThrow()
    val onRaceFunction = instance.getFunction("onRace").orElseThrow()
    val racer = WasmRacer(
        memory = memory,
        moveFunction = { moveFunction.callVoid() },
        onRaceFunction = { onRaceFunction.callVoid() },
    )

    return racer
}

private fun createRacerLinker(
    engine: Engine,
    racerState: RallyRacerState
): Linker<*> {
    // TODO is it correct to use WasiContext here?
    val linker: Linker<WasiContext> = WasiLinkerUtils.createLinker(engine)

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(racerState.throttle) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_throttle_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (throttle) -> racerState.throttle = throttle.asDouble() },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_get",
        FunctionType(arrayOf(), arrayOf(WasmValueType.F64)),
        HostFunction.singleValue { WasmValue.f64(racerState.steering) },
    )

    linker.defineHostFunction(
        "rally_api",
        "controls_steering_set",
        FunctionType(arrayOf(WasmValueType.F64), arrayOf()),
        HostFunction.voidFunction { (steering) -> racerState.steering = steering.asDouble() },
    )

    return linker
}
