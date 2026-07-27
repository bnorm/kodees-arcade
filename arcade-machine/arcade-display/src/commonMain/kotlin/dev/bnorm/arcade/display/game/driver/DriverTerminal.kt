package dev.bnorm.arcade.display.game.driver

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.bnorm.arcade.display.game.TextLines

@Composable
fun DriverTerminal(
    output: TextLines,
    modifier: Modifier = Modifier,
) {
    val vertical = rememberLazyListState()
    val horizontal = rememberScrollState()
    var pinned by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                pinned = when (pinned) {
                    // Scrolled up while pinned => no longer pinned.
                    true -> consumed.y <= 0f
                    // Scrolled all the way to the bottom while not pinned => pinned.
                    false -> consumed.y <= 0f && available.y < 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(output.lines, pinned) {
        if (pinned) {
            // Cancel any current scroll behavior and immediately scroll to the last item.
            try {
                vertical.requestScrollToItem((output.lines.last - output.lines.first).toInt())
            } catch (_: IndexOutOfBoundsException) {
                // TODO this occasionally causes an 'IndexOutOfBoundsException' to be thrown because of a force remeasure...
                //  - why?!?!
                //  - something about the requested item index being the size of the internal list
                //  - like the new item hasn't been added to the lazy column yet...
            }
        }
    }

    Surface(
        color = Color.Black,
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            val scrollbarStyle = LocalScrollbarStyle.current.copy(
                unhoverColor = Color.White.copy(alpha = 0.5f),
                hoverColor = Color.White.copy(alpha = 0.8f),
                minimalHeight = 32.dp,
            )

            val textStyle = MaterialTheme.typography.bodySmall.let {
                it.copy(lineHeight = it.fontSize, fontFamily = FontFamily.Monospace)
            }
            val textModifier = rememberTextMeasurer()
            val density = LocalDensity.current
            val minWidth by derivedStateOf {
                with(density) {
                    textModifier.measure(text = " ".repeat(output.columns), style = textStyle).size.width.toDp()
                }
            }

            ProvideTextStyle(textStyle) {
                // TODO wrap with SelectionContainer
                //  - but this unfortunately crashes
                //  - is there a way to work around this issue?
                LazyColumn(
                    state = vertical,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp + scrollbarStyle.thickness,
                        end = 8.dp + scrollbarStyle.thickness,
                    ),
                    modifier = Modifier
                        .nestedScroll(nestedScrollConnection)
                        .horizontalScroll(horizontal)
                ) {
                    for (line in output.lines) {
                        item(line) {
                            Text(
                                output.getLine(line),
                                modifier = Modifier
                                    .width(minWidth)
                            )
                        }
                    }
                }
            }
            CompositionLocalProvider(
                LocalScrollbarStyle provides scrollbarStyle
            ) {
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(vertical),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = scrollbarStyle.thickness)
                )
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(horizontal),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(end = scrollbarStyle.thickness)
                )
            }
        }
    }
}
