package dev.bnorm.arcade.worker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.ui.Text
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// https://github.com/sindresorhus/cli-spinners/blob/main/spinners.json
private val BRAILLE = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

@Composable
fun BrailleSpinner() {
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(80.milliseconds)
            index = (index + 1) % BRAILLE.size
        }
    }

    Text(value = BRAILLE[index])
}
