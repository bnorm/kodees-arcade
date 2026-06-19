val racers by tasks.registering(Sync::class) {
    into(project.layout.buildDirectory.dir("racers"))

    from(project(":arcade-samples:kodee").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
        include { it.name.endsWith(".wasm") }
        rename { "Kodee.wasm" }
    }
    from(project(":arcade-samples:snail").tasks.named("compileProductionExecutableKotlinWasmWasi")) {
        include { it.name.endsWith(".wasm") }
        rename { "Snail.wasm" }
    }
}
