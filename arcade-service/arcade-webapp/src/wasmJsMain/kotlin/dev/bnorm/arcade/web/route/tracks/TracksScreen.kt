package dev.bnorm.arcade.web.route.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.track.TrackBuilder
import dev.bnorm.arcade.display.track.TrackImage
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.TrackCreateRequest
import dev.bnorm.arcade.service.api.TrackResponse
import dev.bnorm.arcade.web.route.RouteScreen
import dev.bnorm.arcade.web.route.TracksKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
class TracksScreen(
    private val client: ArcadeClient
) : RouteScreen<TracksKey> {
    override val key: KClass<out TracksKey>
        get() = TracksKey::class

    private val tracks = mutableStateListOf<TrackResponse>()

    @Composable
    override fun Content(key: TracksKey) {
        LaunchedEffect(Unit) {
            val apiTracks = client.getTracks()
            tracks.clear()
            tracks.addAll(apiTracks)
        }

        Column {
            Text("Existing Tracks:", style = MaterialTheme.typography.titleLarge)

            LazyRow(
                state = rememberLazyListState(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(tracks) { track ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(track.name)
                        TrackImage(
                            track = track,
                            modifier = Modifier
                                .size(400.dp, 400.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            val scope = rememberCoroutineScope()
            val name = rememberTextFieldState("")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Name:", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    name,
                    isError = name.text.isBlank(),
                )
            }
            TrackBuilder(
                initialSize = IntSize(1000, 1000),
                onSave = {
                    if (name.text.isNotBlank()) {
                        scope.launch {
                            val track = client.createTrack(
                                TrackCreateRequest(
                                    name = name.text.toString(),
                                    width = it.width,
                                    height = it.height,
                                    checkpoints = it.checkpoints,
                                    positions = it.positions,
                                )
                            )
                            tracks.add(track)
                        }
                    }
                }
            )
        }
    }
}
