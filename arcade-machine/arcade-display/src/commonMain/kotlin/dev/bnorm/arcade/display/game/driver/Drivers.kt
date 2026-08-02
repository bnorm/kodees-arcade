package dev.bnorm.arcade.display.game.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .width(264.dp)
                .verticalScroll(vertical)
                .padding(8.dp)
        ) {
            for (driver in model.drivers) {
                key(driver.name) {
                    val isOpen = driver.name in driverDebugViewModel.open
                    val containerColor = animateColorAsState(
                        targetValue = when {
                            isOpen -> MaterialTheme.colorScheme.primary
                            else -> Color.Transparent
                        }
                    )
                    val contentColor = animateColorAsState(
                        targetValue = when {
                            isOpen -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                driverDebugViewModel.open(driver.name)
                            },
                            colors = ButtonDefaults.buttonColors().copy(
                                containerColor = containerColor.value,
                                contentColor = contentColor.value,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize()
                        ) {
                            Text(
                                text = driver.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        AnimatedVisibility(isOpen) {
                            IconButton(
                                onClick = {
                                    // TODO sometimes there's a ui pause
                                    driverDebugViewModel.close(driver.name)
                                }
                            ) {
                                Icon(
                                    imageVector = dev.bnorm.arcade.display.asset.icon.close,
                                    contentDescription = "Close driver window."
                                )
                            }
                        }
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
