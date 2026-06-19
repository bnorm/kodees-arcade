package dev.bnorm.arcade.rally

interface Track {
    val checkpoints: List<Checkpoint>

    interface Checkpoint {
        val start: Point
        val end: Point
    }
}