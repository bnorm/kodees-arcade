package dev.bnorm.arcade.display.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
fun rememberKeyboardState(): KeyboardState {
    return remember { KeyboardState() }
}

class KeyboardState {
    val pressed = mutableStateSetOf<Key>()
}

fun Modifier.onKeyboard(state: KeyboardState): Modifier {
    return onPreviewKeyEvent { event ->
        when (event.type) {
            KeyEventType.KeyDown -> state.pressed.add(event.key)

            KeyEventType.KeyUp,
            KeyEventType.Unknown,
                -> state.pressed.remove(event.key)
        }
        false
    }
}
