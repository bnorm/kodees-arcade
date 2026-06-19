package dev.bnorm.arcade.rally

import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlin.math.exp
import kotlin.math.ln

@Composable
fun LogarithmicSlider(
    initialValue: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    modifier: Modifier = Modifier,
) {
    val minValue = 1f
    var sliderPosition by remember { mutableFloatStateOf(scaleToSlider(initialValue, minValue, maxValue)) }

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onValueChange(sliderToScale(sliderPosition, minValue, maxValue))
            },
            valueRange = 0f..1f,
            steps = steps,
            modifier = modifier,
        )
}

// Convert absolute logarithmic scale to 0.0 - 1.0 fraction
fun scaleToSlider(value: Float, min: Float, max: Float): Float {
    return (ln(value) - ln(min)) / (ln(max) - ln(min))
}

// Convert 0.0 - 1.0 fraction back to logarithmic scale
fun sliderToScale(position: Float, min: Float, max: Float): Float {
    return exp(ln(min) + position * (ln(max) - ln(min)))
}