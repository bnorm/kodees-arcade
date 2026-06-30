package dev.bnorm.arcade.rally.engine.wasm

expect interface WasmEngine

expect inline fun withEngine(block: (WasmEngine) -> Unit)
