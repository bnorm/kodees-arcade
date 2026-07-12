package dev.bnorm.arcade.display.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import dev.bnorm.arcade.Cache
import dev.bnorm.arcade.display.ViewModel
import dev.bnorm.arcade.display.ViewModelCoroutineScope
import dev.bnorm.arcade.geometry.Angle
import dev.bnorm.arcade.geometry.Point
import dev.bnorm.arcade.geometry.Position
import dev.bnorm.arcade.geometry.Segment
import dev.bnorm.arcade.rally.Track
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@Inject
class TrackViewModel(
    @ViewModelCoroutineScope scope: CoroutineScope,
    private val cache: Cache<Track>,
) : ViewModel<TrackViewEvent, TrackModel>(scope) {
    companion object {
        val INITIAL_TRACK = Track(
            width = 500.0,
            height = 400.0,
            checkpoints = listOf(
                Segment(Point(151.0, 170.0), Point(150.0, 80.0)),
                Segment(Point(151.0, 230.0), Point(150.0, 320.0)),
                Segment(Point(349.0, 230.0), Point(350.0, 320.0)),
                Segment(Point(349.0, 170.0), Point(350.0, 80.0)),
            ),
            positions = listOf(
                Position(Point(200.0, 110.0), Angle.HALF_CIRCLE),
                Position(Point(200.0, 140.0), Angle.HALF_CIRCLE),
            ),
        )
    }

    fun new(track: Track) {
        take(TrackViewEvent.New(track))
    }

    @Composable
    override fun models(events: Flow<TrackViewEvent>): TrackModel {
        return TrackPresenter(events, cache)
    }
}

sealed class TrackViewEvent {
    data class New(val track: Track) : TrackViewEvent()
}

data class TrackModel(
    val tracks: List<Track>,
)

@Composable
fun TrackPresenter(
    events: Flow<TrackViewEvent>,
    cache: Cache<Track>,
): TrackModel {
    val keys = remember(cache) {
        mutableStateSetOf<String>().apply {
            addAll(cache.keys)
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is TrackViewEvent.New -> {
                    val key = event.track.hashCode().toString()
                    cache[key] = event.track
                    keys.add(key)
                }
            }
        }
    }

    return TrackModel(
        tracks = keys.mapNotNull { cache[it] },
    )
}
