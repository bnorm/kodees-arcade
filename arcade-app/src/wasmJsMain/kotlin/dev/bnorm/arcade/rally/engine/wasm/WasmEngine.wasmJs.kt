package dev.bnorm.arcade.rally.engine.wasm

actual interface WasmEngine

object WebAssembly : WasmEngine

actual inline fun withEngine(block: (WasmEngine) -> Unit) {
    block(WebAssembly)
}
