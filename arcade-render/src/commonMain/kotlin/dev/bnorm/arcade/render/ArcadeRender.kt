package dev.bnorm.arcade.render

interface ArcadeRender {
    interface Factory {
        fun create(): ArcadeRender
    }

    fun render(data: ByteArray): ByteArray
}
