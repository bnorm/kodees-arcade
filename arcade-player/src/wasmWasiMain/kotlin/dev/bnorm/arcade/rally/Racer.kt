package dev.bnorm.arcade.rally

abstract class Racer {
    open fun onRace(track: Track) {}
    abstract fun move(car: Car, controls: Controls)

    // TODO a racer can "watch" one other racer per "move"
    //  this allows a racer to know the throttle and steering for the other racer
    //  two racers that watch each other, cancel each other out
    //  (this is basically a topological sort for move order of the racers)
    //  what about longer cycles?
    //    should you only be able to watch racers in front of you?
    //    should you automatically watch racers in front of you?
    //  does this actually provide an advantage?
    //    maybe collision prediction in corders or something?

    // TODO we need a "heat" callback
    //  a way to initialize the racer for each race
    //  and a way to preserve data from previous heats
    //    (multiple heats should use the same Wasm instance)
}
