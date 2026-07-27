package dev.bnorm.arcade.rally.engine

import ai.tegmentum.wasmtime4j.RuntimeType
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory

// TODO benchmark wasmtime vs graalwasm?
//  - graalwasm may require some complex setup.
//  - can we write our own FFM bridge? (https://github.com/minamoto79/webasm-java-integration-benchmark)
internal val runtime = WasmRuntimeFactory.create(RuntimeType.PANAMA)

internal val engine = runtime.createEngine(
    EngineConfig.forSize()
        .wasmFunctionReferences(true)
        .wasmGc(true)
        .wasmExceptions(true)
        .wasmComponentModel(true)
)
