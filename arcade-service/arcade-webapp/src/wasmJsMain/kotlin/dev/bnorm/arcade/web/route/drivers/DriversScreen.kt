package dev.bnorm.arcade.web.route.drivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import dev.bnorm.arcade.service.api.DriverCreateRequest
import dev.bnorm.arcade.service.api.DriverResponse
import dev.bnorm.arcade.web.route.DriversKey
import dev.bnorm.arcade.web.route.RouteKey
import dev.bnorm.arcade.web.components.MaxWidthContent
import dev.bnorm.arcade.web.route.RouteScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class, binding<RouteScreen<RouteKey>>())
class DriversScreen(
    private val client: ArcadeClient
) : RouteScreen<DriversKey> {
    override val key: KClass<out DriversKey>
        get() = DriversKey::class

    @Composable
    override fun Content(key: DriversKey) {
        val drivers = remember { mutableStateListOf<DriverResponse>() }
        LaunchedEffect(Unit) {
            val elements = client.getDrivers()
            drivers.clear()
            drivers.addAll(elements)
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {

                    },
                    title = {
                        Text("Drivers")
                    },
                    actions = {
                        CreateDriverButton(client, onCreate = { drivers.add(it) })
                    }
                )
            }
        ) { innerPadding ->
            MaxWidthContent(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                ) {
                    for ((index, driver) in drivers.withIndex()) {
                        DriverCard(driver, client, onUpload = { drivers[index] = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDriverButton(
    client: ArcadeClient,
    onCreate: (DriverResponse) -> Unit
) {
    var displayDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        CreateDriverDialog(
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
        Icon(sports_motorsports, contentDescription = "Create driver")
        Spacer(Modifier.width(4.dp))
        Text("Create", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CreateDriverDialog(
    onDismissRequest: (DriverResponse?) -> Unit,
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
                Text("New Driver", style = MaterialTheme.typography.headlineLarge)
                TextField(
                    state,
                    label = { Text("Name") },
                )
                Button(
                    enabled = state.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val driver = client.createDriver(DriverCreateRequest(state.text.trim().toString()))
                            onDismissRequest(driver)
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

