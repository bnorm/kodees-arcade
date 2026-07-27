package dev.bnorm.arcade.display.game.driver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import dev.bnorm.arcade.display.ViewModel
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.machine.Game
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<Game.DriverDebug>())
class DriverDebugViewModel(
    @ViewModelCoroutineScope scope: CoroutineScope,
    // TODO web has a limit of a single driver?
) : ViewModel<DriverDebugViewEvent, DriverDebugModel>(scope), Game.DriverDebug {
    private val enabled = mutableStateSetOf<String>()

    override fun isEnabled(driver: String): Boolean {
        return driver in enabled
    }

    fun clear() {
        enabled.clear()
    }

    fun enable(driver: String) {
        enabled.add(driver)
    }

    fun disable(driver: String) {
        enabled.remove(driver)
    }

    @Composable
    override fun models(events: Flow<DriverDebugViewEvent>): DriverDebugModel {
        TODO()
    }
}

sealed class DriverDebugViewEvent {
}

class DriverDebugModel(
)
