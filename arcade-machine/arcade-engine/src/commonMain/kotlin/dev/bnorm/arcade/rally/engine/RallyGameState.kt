package dev.bnorm.arcade.rally.engine

class RallyGameState(
    val trackWidth: Double,
    val trackHeight: Double,
    val laps: Int,
    var finished: Boolean,
    var time: Long,
    val driverStates: List<RallyCarState>,
)
