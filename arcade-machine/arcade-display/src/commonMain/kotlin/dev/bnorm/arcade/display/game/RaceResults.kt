package dev.bnorm.arcade.display.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.bnorm.arcade.machine.Game

@Composable
fun RaceResults(event: Game.Event.Complete) {
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
