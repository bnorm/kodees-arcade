package dev.bnorm.arcade.cybertanks

class SpinRightTank : Cybertank() {
    override fun onTurn(tank: Tank) {
        tank.move(Double.POSITIVE_INFINITY)
        tank.turn(Double.POSITIVE_INFINITY)
    }
}
