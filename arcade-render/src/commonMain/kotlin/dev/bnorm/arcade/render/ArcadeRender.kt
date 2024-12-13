package dev.bnorm.arcade.render

import androidx.compose.ui.graphics.drawscope.DrawScope

interface ArcadeRender {
    interface Factory {
        fun create(): ArcadeRender
    }

    fun DrawScope.draw(data: ByteArray)
}
