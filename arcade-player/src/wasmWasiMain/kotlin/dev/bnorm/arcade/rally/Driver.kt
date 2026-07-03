package dev.bnorm.arcade.rally

abstract class Driver {
    open fun onRace(track: Track) {}
    abstract fun move(car: Car, controls: Controls)

    // TODO a driver can "watch" one other driver per "move"
    //  this allows a driver to know the throttle and steering for the other driver
    //  two drivers that watch each other, cancel each other out
    //  (this is basically a topological sort for move order of the drivers)
    //  what about longer cycles?
    //    should you only be able to watch drivers in front of you?
    //    should you automatically watch drivers in front of you?
    //  does this actually provide an advantage?
    //    maybe collision prediction in corders or something?

    // TODO we need a "heat" callback
    //  a way to initialize the driver for each race
    //  and a way to preserve data from previous heats
    //    (multiple heats should use the same Wasm instance)
}
