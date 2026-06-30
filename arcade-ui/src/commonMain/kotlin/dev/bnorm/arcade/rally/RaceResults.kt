package dev.bnorm.arcade.rally

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.bnorm.arcade.machine.Race

@Composable
fun RaceResults(event: Race.Event.Complete) {
    val placed = event.results.entries.sortedBy { it.value.place }
    Column {
        for ((name, result) in placed) {
            Row {
                Text(result.place.toString())
                Text(name)
                Text(result.time.toString())
            }
        }
    }
}
