package dev.bnorm.arcade.rally

import kotlinx.serialization.Serializable

@Serializable
class Car(
    val time: Long,
    val location: Point,
    val velocity: Velocity,
    val nextCheckpoint: Int
)