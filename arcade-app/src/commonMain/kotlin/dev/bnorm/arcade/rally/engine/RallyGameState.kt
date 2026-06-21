package dev.bnorm.arcade.rally.engine

class RallyGameState(
    val trackWidth: Double,
    val trackHeight: Double,
    var finished: Boolean,
    var time: Long,
    val racers: Map<String, RallyRacerState>,
) {
    // TODO hack to make MutableState work...
    override fun equals(other: Any?): Boolean {
        return false
    }
}