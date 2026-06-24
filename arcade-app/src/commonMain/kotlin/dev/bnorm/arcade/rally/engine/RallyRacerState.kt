package dev.bnorm.arcade.rally.engine

import androidx.compose.ui.graphics.ImageBitmap
import dev.bnorm.arcade.geometry.Angle

class RallyRacerState(
    val image: ImageBitmap,
    var x: Double,
    var y: Double,
    var heading: Angle,
    var speed: Double = 0.0,
    var steering: Double = 0.0,
    var throttle: Double = 0.0,
    var checkpoint: Int = 0,
    var lap: Int = 0,
)