package dev.bnorm.arcade.rally.engine

class RallyGameState(
    val trackWidth: Double,
    val trackHeight: Double,
    val finished: Boolean,
    val time: Long,
    val racers: Map<String, RallyCarState>,
)