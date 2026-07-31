package dev.bnorm.arcade.display.internal

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    modifier: Modifier = Modifier,
    stickyRows: Int = 0,
    stickyColumns: Int = 0,
    content: @Composable () -> Unit
) {
    val vertical = remember { ResultsScrollState() }
    val horizontal = remember { ResultsScrollState() }
    Box(modifier = modifier) {
        Layout(
            content = content,
            modifier = Modifier
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
            val layoutHeight = minOf(totalHeight, constraints.maxHeight)
            vertical.updateBounds(totalHeight, layoutHeight)

            val totalWidth = columnsWidths.sum()
            val layoutWidth = minOf(totalWidth, constraints.maxWidth)
            horizontal.updateBounds(totalWidth, layoutWidth)

            layout(layoutWidth, layoutHeight) {
                val vOffset = -vertical.scrollOffset.roundToInt()
                val hOffset = -horizontal.scrollOffset.roundToInt()

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

        Box(modifier = Modifier.matchParentSize()) {
            VerticalScrollbar(
                adapter = vertical,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
            HorizontalScrollbar(
                adapter = horizontal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
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

private class ResultsScrollState : ScrollbarAdapter {
    override var scrollOffset by mutableDoubleStateOf(0.0)
        private set

    override var contentSize by mutableDoubleStateOf(0.0)
        private set
    override var viewportSize by mutableDoubleStateOf(0.0)
        private set

    private val maxScrollBounds: Double
        get() = (contentSize - viewportSize).coerceAtLeast(0.0)

    val scrollableState = ScrollableState { delta ->
        val oldTarget = scrollOffset
        val newTarget = (scrollOffset - delta).coerceIn(0.0, maxScrollBounds)
        scrollOffset = newTarget
        (oldTarget - newTarget).toFloat()
    }

    override suspend fun scrollTo(scrollOffset: Double) {
        scrollableState.scrollBy((this.scrollOffset - scrollOffset).toFloat())
    }

    fun updateBounds(contentSize: Int, viewportSize: Int) {
        this.contentSize = contentSize.toDouble()
        this.viewportSize = viewportSize.toDouble()

        val maxScrollBounds = maxScrollBounds
        if (scrollOffset > maxScrollBounds) {
            scrollOffset = maxScrollBounds
        }
    }
}
