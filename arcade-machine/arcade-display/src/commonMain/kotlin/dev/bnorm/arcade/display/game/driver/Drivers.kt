package dev.bnorm.arcade.display.game.driver

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.game.GameModel
import dev.bnorm.arcade.display.game.GameViewModel

@Composable
fun Drivers(
    model: GameModel,
    driverDebugViewModel: DriverDebugViewModel,
    modifier: Modifier = Modifier,
) {
    val vertical = rememberScrollState()

    Box(
        modifier
            .animateContentSize()
            .fillMaxHeight()
            .width(400.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(vertical)
                .padding(8.dp)
                .padding(end = 8.dp)
        ) {
            for (name in model.start.drivers) {
                key(name) {
                    DriverInfo(name, driverDebugViewModel, model)
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(vertical),
            modifier = Modifier
                .align(Alignment.CenterEnd)
        )
    }
}
