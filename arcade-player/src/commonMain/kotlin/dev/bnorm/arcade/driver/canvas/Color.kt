package dev.bnorm.arcade.driver.canvas

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
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
}
