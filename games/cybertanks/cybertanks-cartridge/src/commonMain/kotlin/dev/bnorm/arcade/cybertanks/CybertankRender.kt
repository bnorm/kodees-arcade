package dev.bnorm.arcade.cybertanks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.bnorm.arcade.cybertanks.CybertankEngineState.Companion.serializer
import dev.bnorm.arcade.render.ArcadeRender
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import kotlin.math.PI

class CybertankRender : ArcadeRender {
    override fun render(data: ByteArray): ByteArray {
        val state = Json.decodeFromString(serializer(), data.decodeToString())

        val drawScope = CanvasDrawScope()
        val size = Size(800f, 600f)
        val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
        val canvas = Canvas(bitmap)

        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = size,
        ) {
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

        return Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes
    }

    class Factory : ArcadeRender.Factory {
        override fun create(): ArcadeRender {
            return CybertankRender()
        }
    }
}
