package dev.bnorm.arcade.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bnorm.arcade.machine.Race
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class RaceViewModel(
    scope: CoroutineScope,
) : ViewModel<RaceViewEvent, RaceModel>(scope) {
    fun clear() {
        take(RaceViewEvent.Clear)
    }

    fun new(race: Race) {
        take(RaceViewEvent.New(race))
    }

    @Composable
    override fun models(events: Flow<RaceViewEvent>): RaceModel {
        return RacePresenter(events)
    }
}

sealed class RaceViewEvent {
    data object Clear : RaceViewEvent()
    data class New(val race: Race) : RaceViewEvent()
}

data class RaceModel(
    val race: Race?,
)

@Composable
fun RacePresenter(
    events: Flow<RaceViewEvent>,
): RaceModel {
    var race by remember { mutableStateOf<Race?>(null) }
    LaunchedEffect(race) {
        // TODO automatically start?
        race?.start()
    }

    LaunchedEffect(events) {
        events.collect {
            when (it) {
                RaceViewEvent.Clear -> {
                    race = null
                }

                is RaceViewEvent.New -> {
                    race = it.race
                }
            }
        }
    }

    return RaceModel(race)
}
