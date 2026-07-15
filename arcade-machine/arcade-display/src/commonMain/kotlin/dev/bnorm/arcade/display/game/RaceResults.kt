package dev.bnorm.arcade.display.game

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.machine.Game

@OptIn(ExperimentalGridApi::class)
@Composable
fun RaceResults(
    results: Map<String, Game.Event.Complete.Result>
) {
    class DriverResult(
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
                    DriverResult(
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

    // TODO we may want a custom layout here
    //  - we want place and name columns to be sticky and only the laps to scroll
    //  - we want to keep the title row sticky and only the drivers scroll
    //  - we want lines between all the "cells"

    Grid(
        config = {
            // Position, Name, Total, Fastest, and Laps
            repeat(4 + numberOfLaps) {
                column(GridTrackSize.Auto)
            }
            // Title and Drivers
            repeat(1 + drivers.size) {
                row(GridTrackSize.Auto)
            }

            gap(8.dp)
        },
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
            Text("Place")
            Text("Driver")
            Text("Total")
            Text("Fastest")
            repeat(numberOfLaps) {
                Text("Lap ${it + 1}")
            }
        }

        val fastestLap = drivers.minOf { it.fastestLap }
        for ((index, driver) in drivers.withIndex()) {
            Text((index + 1).toString(), fontWeight = FontWeight.Bold)
            Text(driver.name, fontWeight = FontWeight.Bold)
            Text(driver.total.toString(), fontWeight = FontWeight.Bold)
            Text(
                driver.fastestLap.toString() + if (driver.fastestLap == fastestLap) "*" else "",
                fontWeight = FontWeight.Bold
            )
            repeat(numberOfLaps) {
                val time = driver.lapTimes.getOrNull(it)
                if (time != null) {
                    Text(
                        time.toString() + if (time == fastestLap) "*" else "",
                        fontWeight = if (time == driver.fastestLap) FontWeight.Bold else FontWeight.Normal
                    )
                } else {
                    Text("DNF")
                }
            }
        }
    }
}
