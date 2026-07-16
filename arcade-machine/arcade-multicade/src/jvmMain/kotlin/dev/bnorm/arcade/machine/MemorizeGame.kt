package dev.bnorm.arcade.machine

class MemorizeGame(
    private val game: Game,
) : Game {
    private var _start: Game.Event.Start? = null
    val start: Game.Event.Start get() = _start ?: error("game not started")

    private var _complete: Game.Event.Complete? = null
    val complete: Game.Event.Complete get() = _complete ?: error("game not completed")

    override suspend fun start(onEvent: suspend (Game.Event) -> Unit) {
        _start = null
        _complete = null
        game.start { event ->
            when (event) {
                is Game.Event.Start -> _start = event
                is Game.Event.Complete -> _complete = event
                else -> Unit
            }
            onEvent(event)
        }
    }
}
