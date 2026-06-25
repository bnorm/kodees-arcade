package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.geometry.Angle

class RallyCarState(
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var checkpoint: Int = 0,
    var lap: Int = 0,
    var finished: Long? = null,
)
