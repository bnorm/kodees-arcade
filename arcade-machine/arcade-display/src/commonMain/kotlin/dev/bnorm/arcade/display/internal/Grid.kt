package dev.bnorm.arcade.display.internal

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

@Composable
fun Grid(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier.Companion,
    stickyRows: Int = 0,
    stickyColumns: Int = 0,
    content: @Composable () -> Unit
) {
    val vertical = remember { ResultsScrollState() }
    val horizontal = remember { ResultsScrollState() }
    Layout(
        content = content,
        modifier = modifier
            .scrollable(vertical.scrollableState, Orientation.Vertical)
            .scrollable(horizontal.scrollableState, Orientation.Horizontal)
            .clip(GridShape) // Clip start and top.
    ) { measurables, constraints ->
        require(measurables.size == columns * rows)

        val intrinsicHeights = measurables.map { it.maxIntrinsicHeight(Int.MAX_VALUE) }
        val intrinsicWidths = measurables.map { it.maxIntrinsicWidth(Int.MAX_VALUE) }

        val rowHeights = IntArray(rows) { r -> (0..<columns).maxOf { c -> intrinsicHeights[r * columns + c] } }
        val columnsWidths = IntArray(columns) { c -> (0..<rows).maxOf { r -> intrinsicWidths[r * columns + c] } }
        val placeables = Array(rows) { r ->
            val height = rowHeights[r]
            Array(columns) { c ->
                val width = columnsWidths[c]
                measurables[r * columns + c].measure(Constraints.fixed(width, height))
            }
        }

        val totalHeight = rowHeights.sum()
        val layoutHeight = minOf(totalHeight / 2, constraints.maxHeight)
        vertical.updateBounds((totalHeight - layoutHeight).coerceAtLeast(0))

        val totalWidth = columnsWidths.sum()
        val layoutWidth = minOf(totalWidth, constraints.maxWidth)
        horizontal.updateBounds((totalWidth - layoutWidth).coerceAtLeast(0))

        layout(layoutWidth, layoutHeight) {
            val vOffset = -vertical.value.roundToInt()
            val hOffset = -horizontal.value.roundToInt()

            var yOffset = 0
            for (r in 0..<rows) {
                var xOffset = 0

                val placeableRow = placeables[r]
                val stickyRow = r < stickyRows
                val y = yOffset + if (stickyRow) 0 else vOffset

                for (c in 0..<columns) {
                    val placeable = placeableRow[c]
                    val stickyColumn = c < stickyColumns
                    val x = xOffset + if (stickyColumn) 0 else hOffset

                    val zIndex = (if (stickyRow) 1f else 0f) + (if (stickyColumn) 1f else 0f)
                    placeable.placeRelative(x, y, zIndex)

                    xOffset += columnsWidths[c]
                }
                yOffset += rowHeights[r]
            }
        }
    }
}

private object GridShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        return Outline.Rectangle(
            Rect(
                left = when (layoutDirection) {
                    LayoutDirection.Ltr -> 0f
                    LayoutDirection.Rtl -> Float.MAX_VALUE
                },
                top = 0f,
                right = when (layoutDirection) {
                    LayoutDirection.Ltr -> Float.MAX_VALUE
                    LayoutDirection.Rtl -> 0f
                },
                bottom = Float.MAX_VALUE,
            )
        )
    }
}

private class ResultsScrollState {
    var value by mutableFloatStateOf(0f)
        private set

    private var maxScrollBounds by mutableFloatStateOf(0f)

    val scrollableState = ScrollableState { delta ->
        val oldTarget = value
        val newTarget = (value - delta).coerceIn(0f, maxScrollBounds)
        value = newTarget
        (oldTarget - newTarget)
    }

    fun updateBounds(maxBounds: Int) {
        maxScrollBounds = maxBounds.toFloat()
        if (value > maxScrollBounds) {
            value = maxScrollBounds
        }
    }
}
