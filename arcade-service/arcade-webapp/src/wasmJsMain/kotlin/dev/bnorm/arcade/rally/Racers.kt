package dev.bnorm.arcade.rally

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.bnorm.arcade.icons.sports_motorsports
import dev.bnorm.arcade.icons.upload
import dev.bnorm.arcade.route.Route
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RacerCreateRequest
import dev.bnorm.arcade.service.api.RacerId
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.service.api.Version
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
class RacersRoute(
    private val client: ArcadeClient
) : Route {
    override val path: String get() = "/racers"

    @Composable
    override fun Content() {
        val racers = remember { mutableStateListOf<RacerResponse>() }
        LaunchedEffect(Unit) {
            val elements = client.getRacers()
            racers.clear()
            racers.addAll(elements)
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Racers", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                CreateRacerButton(client, onCreate = { racers.add(it) })
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .width(IntrinsicSize.Max)
            ) {
                for ((index, racer) in racers.withIndex()) {
                    RacerCard(racer, client, onUpload = { racers[index] = it })
                }
            }
        }
    }
}

@Composable
private fun CreateRacerButton(
    client: ArcadeClient,
    onCreate: (RacerResponse) -> Unit
) {
    var displayDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        CreateRacerDialog(
            onDismissRequest = {
                if (it != null) onCreate(it)
                displayDialog = false
            },
            client = client,
        )
    }

    TextButton(
        onClick = { displayDialog = true }
    ) {
        Icon(sports_motorsports, contentDescription = "Create racer")
        Spacer(Modifier.width(4.dp))
        Text("Create", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CreateRacerDialog(
    onDismissRequest: (RacerResponse?) -> Unit,
    client: ArcadeClient,
) {
    Dialog(
        onDismissRequest = { onDismissRequest(null) },
    ) {
        val scope = rememberCoroutineScope()
        val state = rememberTextFieldState()

        Card {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text("New Racer", style = MaterialTheme.typography.headlineLarge)
                TextField(
                    state,
                    label = { Text("Name") },
                )
                Button(
                    enabled = state.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val racer = client.createRacer(RacerCreateRequest(state.text.trim().toString()))
                            onDismissRequest(racer)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@Composable
private fun RacerCard(
    racer: RacerResponse,
    client: ArcadeClient,
    onUpload: (RacerResponse) -> Unit,
) {
    val latest = racer.versions.lastOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        racer.name,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (latest != null) {
                        Text(
                            "($latest)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
                Spacer(Modifier.weight(1f).widthIn(min = 32.dp))
                UploadRacerButton(client, racer.id, onUpload = onUpload)
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                for (version in racer.versions.asReversed()) {
                    Text(version.toString())
                }
            }
        }
    }
}

@Composable
private fun UploadRacerButton(
    client: ArcadeClient,
    id: RacerId,
    onUpload: (RacerResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        UploadRacerDialog(
            client = client,
            id = id,
            onDismissRequest = {
                if (it != null) onUpload(it)
                displayDialog = false
            },
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                indication = ripple(bounded = false, radius = 24.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                displayDialog = true
            }
    ) {
        // TODO this doesn't look truly centered...
        Icon(upload, contentDescription = "Upload racer")
    }
}

@Composable
private fun UploadRacerDialog(client: ArcadeClient, id: RacerId, onDismissRequest: (RacerResponse?) -> Unit) {
    Dialog(
        onDismissRequest = { onDismissRequest(null) },
    ) {
        val scope = rememberCoroutineScope()
        val state = rememberTextFieldState()
        var file by remember { mutableStateOf<PlatformFile?>(null) }
        val launcher = rememberFilePickerLauncher(
            mode = PickerMode.Single,
            type = PickerType.File(listOf("wasm")),
        ) { file = it }


        Card {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Upload Version", style = MaterialTheme.typography.headlineLarge)
                TextField(state, label = { Text("Version") })
                Button(
                    onClick = { launcher.launch() },
                ) {
                    Text("Select")
                }
                Button(
                    enabled = state.text.isNotBlank() && file != null,
                    onClick = {
                        scope.launch {
                            // TODO protect against bad version strings
                            val version = Version.parse(state.text.toString())
                            val bytes = file!!.readBytes()
                            val racer = client.uploadRacerVersion(id, version, bytes)
                            onDismissRequest(racer)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text("Upload")
                }
            }
        }
    }
}
