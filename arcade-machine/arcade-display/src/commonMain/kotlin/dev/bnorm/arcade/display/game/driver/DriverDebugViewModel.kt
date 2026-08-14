package dev.bnorm.arcade.display.game.driver

import androidx.compose.runtime.mutableStateSetOf
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.machine.Game
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<Game.DriverDebug>())
class DriverDebugViewModel(
    @ViewModelCoroutineScope private val scope: CoroutineScope,
) : Game.DriverDebug {
    val open: Set<String>
        field = mutableStateSetOf<String>()
    val drawing: Set<String>
        field = mutableStateSetOf<String>()

    val focusRequests: Flow<FocusRequest>
        field = MutableSharedFlow()

    class FocusRequest(val driver: String)

    override fun isDrawingEnabled(driver: String): Boolean {
        return driver in drawing
    }

    fun clear() {
        open.clear()
        drawing.clear()
    }

    fun open(driver: String) {
        open.add(driver)
        scope.launch {
            focusRequests.emit(FocusRequest(driver))
        }
    }

    fun close(driver: String) {
        open.remove(driver)
    }

    fun startDrawing(driver: String) {
        drawing.add(driver)
    }

    fun stopDrawing(driver: String) {
        drawing.remove(driver)
    }
}
