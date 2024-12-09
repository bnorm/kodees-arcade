package dev.bnorm.arcade.cybertanks

class SpinLeftTank : Cybertank() {
    override fun onTurn(tank: Tank) {
        tank.move(Double.POSITIVE_INFINITY)
        tank.turn(Double.NEGATIVE_INFINITY)
    }
}
