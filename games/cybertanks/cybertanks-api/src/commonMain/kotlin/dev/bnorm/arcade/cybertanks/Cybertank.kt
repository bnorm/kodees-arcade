package dev.bnorm.arcade.cybertanks

import dev.bnorm.arcade.agent.ArcadeAgent

abstract class Cybertank : ArcadeAgent {
    fun onStart(tank: Tank) {}
    abstract fun onTurn(tank: Tank)
}
