package dev.bnorm.arcade.cybertanks

interface Tank : Vector {
    val remainingTurn: Double
    fun turn(angle: Double)
    val remainingMove: Double
    fun move(distance: Double)

    val gunHeading: Double
    val remainingGunTurn: Double
    fun turnGun(angle: Double)
    fun fire(power: Double): Bullet

    val radarHeading: Double
    val remainingRadarTurn: Double
    fun turnRadar(angle: Double)
}
