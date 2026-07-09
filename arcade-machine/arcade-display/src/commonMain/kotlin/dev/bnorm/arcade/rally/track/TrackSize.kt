package dev.bnorm.arcade.rally.track

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun TrackSize(
    initialSize: IntSize,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Size:", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.width(16.dp))

        val width = rememberTextFieldState(initialSize.width.toString())
        val widthInt = width.text.toString().toIntOrNull()
        OutlinedTextField(
            state = width,
            label = { Text("Width") },
            isError = widthInt == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        val height = rememberTextFieldState(initialSize.height.toString())
        val heightInt = height.text.toString().toIntOrNull()
        OutlinedTextField(
            state = height,
            label = { Text("Height") },
            isError = heightInt == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        LaunchedEffect(widthInt, heightInt) {
            if (widthInt != null && heightInt != null) {
                onSizeChanged(IntSize(widthInt, heightInt))
            }
        }
    }
}
