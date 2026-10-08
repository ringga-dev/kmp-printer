package ngga.ring.printer.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard breakpoints for responsive UI in Compose Multiplatform.
 * Zero external dependencies: purely built using Kotlin and Compose Dp primitives.
 */
enum class WindowWidthSizeClass {
    /** Smartphones, portrait mode, narrow split screens (< 600dp) */
    Compact,
    /** Foldables, 7"-10" tablets, POS terminal stands (600dp - 840dp) */
    Medium,
    /** Desktop screens, 12"+ tablets, large POS kiosks, web browsers (> 840dp) */
    Expanded;

    companion object {
        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

/**
 * Encapsulates responsive window size classes.
 */
@Immutable
data class WindowSize(
    val widthClass: WindowWidthSizeClass,
    val width: Dp,
    val height: Dp
) {
    val isCompact: Boolean get() = widthClass == WindowWidthSizeClass.Compact
    val isMedium: Boolean get() = widthClass == WindowWidthSizeClass.Medium
    val isExpanded: Boolean get() = widthClass == WindowWidthSizeClass.Expanded
}
