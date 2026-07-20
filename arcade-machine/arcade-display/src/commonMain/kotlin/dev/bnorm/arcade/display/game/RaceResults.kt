package dev.bnorm.arcade.display.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.internal.Grid
import dev.bnorm.arcade.machine.Game

@Composable
fun RaceResults(
    results: Map<String, Game.Event.Complete.Result>,
    modifier: Modifier = Modifier,
) {
    class DriverRow(
        val name: String,
        val total: Long,
        val fastestLap: Long,
        val lapTimes: List<Long>,
    )

    val numberOfLaps = remember(results) {
        results.maxOf { it.value.laps.size }
    }

    val drivers = remember(results) {
        buildList {
            for ((name, result) in results.entries) {
                val lapTimes = buildList {
                    var last = 0L
                    for (time in result.laps) {
                        add(time - last)
                        last = time
                    }
                }

                add(
                    DriverRow(
                        name = name,
                        total = result.laps.getOrNull(numberOfLaps - 1) ?: -1L,
                        fastestLap = lapTimes.minOrNull() ?: -1L,
                        lapTimes = lapTimes,
                    )
                )
            }

            sortBy { if (it.total < 0) Long.MAX_VALUE else it.total }
        }
    }

    @Composable
    fun Item(text: String) {
        Text(
            text = text,
            modifier = Modifier
                .border(width = 1.dp, color = Color.Black)
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        )
    }

    Grid(
        rows = 1 + drivers.size,
        stickyRows = 1,
        columns = 4 + numberOfLaps,
        stickyColumns = 4,
        modifier = modifier
    ) {
        ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
            Item("Place")
            Item("Driver")
            Item("Total")
            Item("Fastest")
            repeat(numberOfLaps) {
                Item("Lap ${it + 1}")
            }
        }

        val fastestLap = drivers.minOf { it.fastestLap }
        for ((index, driver) in drivers.withIndex()) {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
                Item((index + 1).toString())
                Item(driver.name)
                Item(driver.total.toString())
                Item(driver.fastestLap.toString() + if (driver.fastestLap == fastestLap) "*" else "")
            }
            repeat(numberOfLaps) {
                when (val time = driver.lapTimes.getOrNull(it)) {
                    null ->
                        Item("DNF")

                    driver.fastestLap ->
                        ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
                            Item(time.toString() + if (time == fastestLap) "*" else "")
                        }

                    else ->
                        Item(time.toString() + if (time == fastestLap) "*" else "")
                }
            }
        }
    }
}
