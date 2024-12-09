package dev.bnorm.arcade.cybertanks

class TankState(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var heading: Double = 0.0,
) : Tank {
    override var velocity: Double = 0.0

    override var remainingTurn: Double = 0.0
    override fun turn(angle: Double) {
        remainingTurn = angle
    }

    override var remainingMove: Double = 0.0
    override fun move(distance: Double) {
        remainingMove = distance
    }

    override var gunHeading: Double = heading
    override var remainingGunTurn: Double = 0.0
    override fun turnGun(angle: Double) {
        remainingGunTurn = angle
    }

    override fun fire(power: Double): Bullet {
        TODO("Not yet implemented")
    }

    override var radarHeading: Double = heading
    override var remainingRadarTurn: Double = 0.0
    override fun turnRadar(angle: Double) {
        remainingRadarTurn = angle
    }
}
