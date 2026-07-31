package dev.bnorm.arcade.display.game.driver

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.game.GameModel

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
            .width(IntrinsicSize.Max)
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
                    val isOpen = name in driverDebugViewModel.open
                    Button(
                        colors = ButtonDefaults.buttonColors().copy(
                            if (isOpen) MaterialTheme.colorScheme.primary else Color.Transparent,
                            if (isOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        ),
                        onClick = {
                            driverDebugViewModel.open(name)
                        },
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        )
                    }
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
