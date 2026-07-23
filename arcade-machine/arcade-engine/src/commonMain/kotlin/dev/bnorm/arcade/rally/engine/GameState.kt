package dev.bnorm.arcade.rally.engine

import dev.bnorm.arcade.driver.Track

class GameState(
    val track: Track,
    val laps: Int,
    var finished: Boolean,
    var time: Long,
    val driverStates: List<DriverState>,
) {
    val widthRange = borderImpactDist..track.width - borderImpactDist
    val heightRange = borderImpactDist..track.height - borderImpactDist
}
