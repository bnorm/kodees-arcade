package dev.bnorm.arcade.display.game.driver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.game.GameModel
import dev.bnorm.arcade.display.game.GameViewModel
import dev.bnorm.arcade.display.game.TextLines

@Composable
fun DriverInfo(
    name: String,
    debug: SnapshotStateSet<String>,
    gameViewModel: GameViewModel,
    model: GameModel
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val checked = name in debug
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        when (checked) {
                            true -> {
                                debug.remove(name)
                                gameViewModel.setDebug(name, false)
                            }

                            false -> {
                                debug.add(name)
                                gameViewModel.setDebug(name, true)
                            }
                        }
                    }
                )
                Text("Debug", style = MaterialTheme.typography.bodyLarge)
            }
            DriverTerminal(
                output = model.driverOutput[name] ?: TextLines(),
                modifier = Modifier
                    .height(200.dp)
            )
        }
    }
}
