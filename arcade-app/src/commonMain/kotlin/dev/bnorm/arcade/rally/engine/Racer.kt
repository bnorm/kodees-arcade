package dev.bnorm.arcade.rally.engine

interface Racer {
    val name: String
    val bytes: ByteArray
}

class ByteArrayRacer(
    override val name: String,
    override val bytes: ByteArray
) : Racer
