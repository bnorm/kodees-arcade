package dev.bnorm.arcade.rally

import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Vector
import kotlinx.serialization.Serializable

@Serializable
class Car(
    val time: Long,
    val location: Point,
    val velocity: Vector,
    val lap: Int,
    val nextCheckpoint: Int,
)