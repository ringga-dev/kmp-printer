package ngga.ring.printer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ngga.ring.printer.ui.adaptive.WindowWidthSizeClass
import ngga.ring.printer.ui.theme.ReceiptPaperTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReceiptPaperThemeTest {

    @Test
    fun testDefaultClassicThermalTheme() {
        val theme = ReceiptPaperTheme.ClassicThermal
        assertEquals(Color(0xFFFDFBF7), theme.paperColor)
        assertEquals(Color(0xFF1E1E1E), theme.textColor)
        assertTrue(theme.showCutEdgeZigzag)
    }

    @Test
    fun testDarkTerminalTheme() {
        val theme = ReceiptPaperTheme.DarkTerminal
        assertEquals(Color(0xFF18181B), theme.paperColor)
        assertEquals(Color(0xFF4ADE80), theme.textColor)
        assertEquals(Color(0xFF4ADE80), theme.invertedBackgroundColor)
        assertEquals(Color(0xFF18181B), theme.invertedTextColor)
    }

    @Test
    fun testWindowSizeClassBreakpoints() {
        assertEquals(WindowWidthSizeClass.Compact, WindowWidthSizeClass.fromWidth(400.dp))
        assertEquals(WindowWidthSizeClass.Medium, WindowWidthSizeClass.fromWidth(720.dp))
        assertEquals(WindowWidthSizeClass.Expanded, WindowWidthSizeClass.fromWidth(1024.dp))
    }
}
