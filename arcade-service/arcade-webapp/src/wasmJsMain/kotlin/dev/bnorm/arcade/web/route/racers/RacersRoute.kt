package dev.bnorm.arcade.web.route.racers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.RacerCreateRequest
import dev.bnorm.arcade.service.api.RacerResponse
import dev.bnorm.arcade.web.route.Route
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
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

