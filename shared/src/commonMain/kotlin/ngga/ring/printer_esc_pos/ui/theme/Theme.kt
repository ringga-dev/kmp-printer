package ngga.ring.printer_esc_pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PrinterColors.ElectricBlue,
    onPrimary = Color.Black,
    primaryContainer = PrinterColors.ElectricBlueDark,
    onPrimaryContainer = Color.White,
    secondary = PrinterColors.Emerald,
    onSecondary = Color.Black,
    background = PrinterColors.DeepSpace,
    surface = PrinterColors.SurfaceDark,
    onSurface = PrinterColors.TextPrimary,
    onSurfaceVariant = PrinterColors.TextSecondary,
    outline = PrinterColors.BorderDefault,
    error = PrinterColors.ErrorRed
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PrinterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Current app is optimized for Dark Mode, but extensible
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content
    )
}
