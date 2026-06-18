package dev.bnorm.arcade.rally

interface Car : Point, Vector {
    val time: Long

    val nextCheckpoint: Int
}
