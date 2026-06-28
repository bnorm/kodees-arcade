package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.Linker
import ai.tegmentum.wasmtime4j.WasmValue
import ai.tegmentum.wasmtime4j.WasmValueType
import ai.tegmentum.wasmtime4j.func.HostFunction
import ai.tegmentum.wasmtime4j.type.FunctionType
import ai.tegmentum.wasmtime4j.wasi.WasiContext
import dev.bnorm.arcade.rally.engine.RacerControlState

actual suspend fun WasmEngine.createWasmRacer(
    controlState: RacerControlState,
    racer: ByteArray,
    name: String
): WasmRacer {
    val linker = createRacerLinker(this, controlState)
    val module = compileModule(racer)
    val store = createStore()
    val instance = linker.instantiate(store, module)

    val memory = Wasmtime4jMemory(instance.defaultMemory.orElseThrow())
    val moveFunction = instance.getFunction("move").orElseThrow()
    val onRaceFunction = instance.getFunction("onRace").orElseThrow()
    val racer = WasmRacer(
        name = name,
        memory = memory,
        moveFunction = { moveFunction.callVoid() },
        onRaceFunction = { onRaceFunction.callVoid() },
        onClose = {
            instance.close()
            store.close()
            module.close()
            linker.close()
        }
    )

    return racer
}

private fun createRacerLinker(
    engine: Engine,
    controlState: RacerControlState,
): Linker<*> {
    val runtime = engine.runtime
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

    return linker
}
