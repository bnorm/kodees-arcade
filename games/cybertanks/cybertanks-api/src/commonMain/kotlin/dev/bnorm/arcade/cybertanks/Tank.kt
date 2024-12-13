package dev.bnorm.arcade.cybertanks

interface Tank : Vector {
    val remainingTurn: Angle
    fun turn(angle: Angle)
    val remainingMove: Double
    fun move(distance: Double)

    val gunHeading: Angle
    val remainingGunTurn: Angle
    fun turnGun(angle: Angle)
    fun fire(power: Double): Bullet

    val radarHeading: Angle
    val remainingRadarTurn: Angle
    fun turnRadar(angle: Angle)
}
