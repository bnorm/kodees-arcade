package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle

class RallyCarState(
    val x: Double,
    val y: Double,
    val heading: Angle,
    val speed: Double = 0.0,
    val checkpoint: Int = 0,
    val lap: Int = 0,
) {
    class Mutable(original: RallyCarState) {
        var x: Double = original.x
        var y: Double = original.y
        var heading: Angle = original.heading
        var speed: Double = original.speed
        var checkpoint: Int = original.checkpoint
        var lap: Int = original.lap

        fun build(): RallyCarState {
            return RallyCarState(
                x = x,
                y = y,
                heading = heading,
                speed = speed,
                checkpoint = checkpoint,
                lap = lap,
            )
        }
    }
}