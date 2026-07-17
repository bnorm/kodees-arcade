package dev.bnorm.arcade.web.route.tracks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.track.TrackBuilder
import dev.bnorm.arcade.display.track.TrackSize
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.TrackCreateRequest
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.route.RouteScreen
import dev.bnorm.arcade.web.route.TracksKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class, binding<RouteScreen<RouteKey>>())
class TracksScreen(
    private val client: ArcadeClient
) : RouteScreen<TracksKey> {
    override val key: KClass<out TracksKey>
        get() = TracksKey::class

    @Composable
    override fun Content(key: TracksKey) {
        val scope = rememberCoroutineScope()

        Column {
            val name = rememberTextFieldState("")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Name:", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    name,
                    isError = name.text.isBlank(),
                )
            }
            var size by remember { mutableStateOf(IntSize(1000, 1000)) }
            TrackSize(size, onSizeChanged = { size = it })
            TrackBuilder(
                size,
                onSave = {
                    if (name.text.isNotBlank()) {
                        scope.launch {
                            client.createTrack(
                                TrackCreateRequest(
                                    name = name.text.toString(),
                                    width = it.width,
                                    height = it.height,
                                    checkpoints = it.checkpoints,
                                    positions = it.positions,
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}
