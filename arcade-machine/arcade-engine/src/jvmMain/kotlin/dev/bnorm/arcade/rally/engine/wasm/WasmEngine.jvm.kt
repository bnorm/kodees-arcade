package dev.bnorm.arcade.rally.engine.wasm

import ai.tegmentum.wasmtime4j.Engine
import ai.tegmentum.wasmtime4j.config.EngineConfig
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory

actual typealias WasmEngine = Engine

@PublishedApi
internal val runtime = WasmRuntimeFactory.create()

@PublishedApi
internal val engine = runtime.createEngine(
    EngineConfig.forSize()
        .wasmFunctionReferences(true)
        .wasmGc(true)
        .wasmExceptions(true)
        .wasmComponentModel(true)
)

actual inline fun withEngine(block: (WasmEngine) -> Unit) {
    block(engine)
}
