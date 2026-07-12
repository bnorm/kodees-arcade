package dev.bnorm.arcade.driver.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DrawStyle

@SerialName("Fill")
@Serializable
data object Fill : DrawStyle()

@SerialName("Stroke")
@Serializable
data class Stroke(
    val width: Float = 0.0f,
) : DrawStyle() {
    companion object {

        /** Width to indicate a hairline stroke of 1 pixel */
        const val HairlineWidth = 0.0f
    }
}
