package ngga.ring.printer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Customizable theme for rendering thermal receipts and tickets in Compose UI.
 */
@Immutable
data class ReceiptPaperTheme(
    /** Paper background color (default warm ivory thermal paper) */
    val paperColor: Color = Color(0xFFFDFBF7),
    /** Text color (default charcoal thermal black) */
    val textColor: Color = Color(0xFF1E1E1E),
    /** Color for divider lines and borders */
    val dividerColor: Color = Color(0xFFD4D4D8),
    /** Shadow elevation around the paper sheet */
    val shadowElevation: Dp = 4.dp,
    /** Corner radius of the paper container */
    val cornerRadius: Dp = 6.dp,
    /** Whether to draw the jagged zigzag tear-off effect at top/bottom */
    val showCutEdgeZigzag: Boolean = true,
    /** Monospace font family for thermal printing authenticity */
    val fontFamily: FontFamily = FontFamily.Monospace,
    /** Inverted block text background color */
    val invertedBackgroundColor: Color = Color(0xFF1E1E1E),
    /** Inverted block text foreground color */
    val invertedTextColor: Color = Color(0xFFFDFBF7)
) {
    companion object {
        /** Classic warm thermal receipt style */
        val ClassicThermal = ReceiptPaperTheme()

        /** Crisp modern white receipt style */
        val FreshWhite = ReceiptPaperTheme(
            paperColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF09090B),
            dividerColor = Color(0xFFE4E4E7),
            shadowElevation = 2.dp
        )

        /** High-contrast dark terminal style */
        val DarkTerminal = ReceiptPaperTheme(
            paperColor = Color(0xFF18181B),
            textColor = Color(0xFF4ADE80),
            dividerColor = Color(0xFF27272A),
            shadowElevation = 0.dp,
            invertedBackgroundColor = Color(0xFF4ADE80),
            invertedTextColor = Color(0xFF18181B)
        )
    }
}
