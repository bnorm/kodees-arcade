package dev.bnorm.arcade.web.route.tracks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.TrackCreateRequest
import dev.bnorm.arcade.web.route.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
class TracksRoute(
    private val client: ArcadeClient
) : Route {
    override val path: String get() = "/tracks"

    @Composable
    override fun Content() {
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

@Composable
fun TrackSize(
    initialSize: IntSize,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Size:", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.width(16.dp))

        val width = rememberTextFieldState(initialSize.width.toString())
        val widthInt = width.text.toString().toIntOrNull()
        OutlinedTextField(
            state = width,
            label = { Text("Width") },
            isError = widthInt == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        val height = rememberTextFieldState(initialSize.height.toString())
        val heightInt = height.text.toString().toIntOrNull()
        OutlinedTextField(
            state = height,
            label = { Text("Height") },
            isError = heightInt == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        LaunchedEffect(widthInt, heightInt) {
            if (widthInt != null && heightInt != null) {
                onSizeChanged(IntSize(widthInt, heightInt))
            }
        }
    }
}
