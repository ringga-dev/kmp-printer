package ngga.ring.printer.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Responsive layout container that calculates the current [WindowSize] and passes it to [content].
 * Adapts seamlessly across Mobile, Tablet, Desktop, and Web with zero extra dependencies.
 */
@Composable
fun AdaptiveBox(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSize) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val windowSize = WindowSize(
            widthClass = WindowWidthSizeClass.fromWidth(maxWidth),
            width = maxWidth,
            height = maxHeight
        )
        content(windowSize)
    }
}
