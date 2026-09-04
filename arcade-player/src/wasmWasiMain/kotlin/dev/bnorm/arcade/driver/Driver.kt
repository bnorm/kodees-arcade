package dev.bnorm.arcade.driver

import dev.bnorm.arcade.driver.canvas.Canvas

abstract class Driver {
    open fun onRace(race: Race) {}
    abstract fun onTurn(car: Car, controls: Controls)

    open fun onCar(car: Car) {}

    open fun onDraw(canvas: Canvas) {}
}
