package dev.bnorm.arcade.cybertanks

class TankState(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var heading: Angle = Angle.ZERO,
) : Tank {
    override var speed: Double = 0.0

    override var remainingTurn: Angle = Angle.ZERO
    override fun turn(angle: Angle) {
        remainingTurn = angle
    }

    override var remainingMove: Double = 0.0
    override fun move(distance: Double) {
        remainingMove = distance
    }

    override var gunHeading: Angle = heading
    override var remainingGunTurn: Angle = Angle.ZERO
    override fun turnGun(angle: Angle) {
        remainingGunTurn = angle
    }

    override fun fire(power: Double): Bullet {
        TODO("Not yet implemented")
    }

    override var radarHeading: Angle = heading
    override var remainingRadarTurn: Angle = Angle.ZERO
    override fun turnRadar(angle: Angle) {
        remainingRadarTurn = angle
    }
}
