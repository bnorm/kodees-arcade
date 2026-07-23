package dev.bnorm.arcade.driver.canvas

import kotlin.jvm.JvmInline

@JvmInline
value class Color(val value: UInt) {
    constructor(
        red: UByte,
        green: UByte,
        blue: UByte,
        alpha: UByte = 0xFFu,
    ) : this(
        (alpha.toUInt() shl 24) or
            (red.toUInt() shl 16) or
            (green.toUInt() shl 8) or
            blue.toUInt()
    )

    companion object {
        val Black = Color(0xFF000000u)
        val DarkGray = Color(0xFF444444u)
        val Gray = Color(0xFF888888u)
        val LightGray = Color(0xFFCCCCCCu)
        val White = Color(0xFFFFFFFFu)
        val Red = Color(0xFFFF0000u)
        val Green = Color(0xFF00FF00u)
        val Blue = Color(0xFF0000FFu)
        val Yellow = Color(0xFFFFFF00u)
        val Cyan = Color(0xFF00FFFFu)
        val Magenta = Color(0xFFFF00FFu)
        val Transparent = Color(0x00000000u)
    }
}
