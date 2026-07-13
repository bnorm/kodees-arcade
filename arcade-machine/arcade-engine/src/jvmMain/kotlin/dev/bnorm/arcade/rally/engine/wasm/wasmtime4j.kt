package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.RuntimeType
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory

internal val runtime = WasmRuntimeFactory.create(RuntimeType.PANAMA)

internal val engine = runtime.createEngine(
    EngineConfig.forSize()
        .wasmFunctionReferences(true)
        .wasmGc(true)
        .wasmExceptions(true)
        .wasmComponentModel(true)
)
