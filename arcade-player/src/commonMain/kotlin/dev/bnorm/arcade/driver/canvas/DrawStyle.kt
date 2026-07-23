package dev.bnorm.arcade.driver.canvas

sealed class DrawStyle

data object Fill : DrawStyle()

data class Stroke(
    val width: Float = 0.0f,
) : DrawStyle() {
    companion object {

        /** Width to indicate a hairline stroke of 1 pixel */
        const val HairlineWidth = 0.0f
    }
}
