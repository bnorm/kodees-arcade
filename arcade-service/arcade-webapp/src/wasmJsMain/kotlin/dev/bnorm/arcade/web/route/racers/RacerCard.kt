package dev.bnorm.arcade.web.route.racers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.bnorm.arcade.icons.download
import dev.bnorm.arcade.icons.upload
import dev.bnorm.arcade.server.client.ArcadeClient
import dev.bnorm.arcade.service.api.DriverId
import dev.bnorm.arcade.service.api.DriverResponse
import dev.bnorm.arcade.service.api.Version
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.download
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

@Composable
fun DriverCard(
    driver: DriverResponse,
    client: ArcadeClient,
    onUpload: (DriverResponse) -> Unit,
) {
    val versions = remember { mutableStateListOf<Version>() }
    LaunchedEffect(driver) {
        val sorted = client.getDriverVersions(driver.id).map { it.version }.sorted()
        versions.clear()
        versions.addAll(sorted)
    }
    val latest = versions.lastOrNull()

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
                        driver.name,
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
                UploadDriverButton(client, driver.id, onUpload = onUpload)
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                for (version in versions.asReversed()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(version.toString(), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        DownloadDriverButton(client, driver, version)
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadDriverButton(
    client: ArcadeClient,
    id: DriverId,
    onUpload: (DriverResponse) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    var displayDialog by remember { mutableStateOf(false) }
    if (displayDialog) {
        UploadDriverDialog(
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
        Icon(upload, contentDescription = "Upload driver")
    }
}

@Composable
private fun UploadDriverDialog(client: ArcadeClient, id: DriverId, onDismissRequest: (DriverResponse?) -> Unit) {
    Dialog(
        onDismissRequest = { onDismissRequest(null) },
    ) {
        val scope = rememberCoroutineScope()
        val state = rememberTextFieldState()
        var file by remember { mutableStateOf<PlatformFile?>(null) }
        val launcher = rememberFilePickerLauncher(
            mode = FileKitMode.Single,
            type = FileKitType.File("wasm"),
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
                            val driver = client.uploadDriverVersion(id, version, bytes)
                            onDismissRequest(driver)
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

@Composable
private fun DownloadDriverButton(
    client: ArcadeClient,
    driver: DriverResponse,
    version: Version,
    modifier: Modifier = Modifier.Companion
) {
    val scope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                indication = ripple(bounded = false, radius = 16.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                scope.launch {
                    FileKit.download(
                        bytes = client.downloadDriverVersion(driver.id, version),
                        fileName = "${driver.name} $version.wasm"
                    )
                }
            }
    ) {
        // TODO this doesn't look truly centered...
        Icon(download, contentDescription = "Download driver", modifier = Modifier.size(16.dp))
    }
}
