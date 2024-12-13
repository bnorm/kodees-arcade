package dev.bnorm.arcade.cybertanks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import dev.bnorm.arcade.cybertanks.CybertankEngineState.Companion.serializer
import dev.bnorm.arcade.render.ArcadeRender
import kotlinx.serialization.json.Json
import kotlin.math.PI

class CybertankRender : ArcadeRender {
    override fun DrawScope.draw(data: ByteArray) {
        val state = Json.decodeFromString(serializer(), data.decodeToString())

        // Draw background.
        drawRect(color = Color.Black, topLeft = Offset.Zero, size = size)

        // Draw tanks.
        for (tank in state.tanks) {
            val x = tank.x.toFloat()
            val y = size.height - tank.y.toFloat()
            rotate(
                degrees = (tank.heading * 180.0 / PI).toFloat(),
                pivot = Offset(x, y)
            ) {
                drawRect(Color.Red, Offset(x - 16.0f, y - 16.0f), Size(32.0f, 32.0f))
            }
        }
    }

    class Factory : ArcadeRender.Factory {
        override fun create(): ArcadeRender {
            return CybertankRender()
        }
    }
}
