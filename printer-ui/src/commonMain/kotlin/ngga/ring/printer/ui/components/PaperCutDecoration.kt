package ngga.ring.printer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a realistic jagged/zigzag tear-off edge at the top or bottom of thermal receipt paper.
 * Pure Compose Canvas path drawing with zero external dependencies.
 */
@Composable
fun PaperCutDecoration(
    color: Color,
    modifier: Modifier = Modifier,
    toothWidth: Dp = 8.dp,
    toothHeight: Dp = 5.dp,
    isTop: Boolean = false
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(toothHeight)
    ) {
        val widthPx = size.width
        val heightPx = size.height
        val toothWidthPx = toothWidth.toPx()
        val numTeeth = (widthPx / toothWidthPx).toInt().coerceAtLeast(1)
        val actualToothWidth = widthPx / numTeeth

        val path = Path()
        if (isTop) {
            path.moveTo(0f, heightPx)
            for (i in 0 until numTeeth) {
                val startX = i * actualToothWidth
                val midX = startX + (actualToothWidth / 2f)
                val endX = startX + actualToothWidth
                path.lineTo(midX, 0f)
                path.lineTo(endX, heightPx)
            }
            path.close()
        } else {
            path.moveTo(0f, 0f)
            for (i in 0 until numTeeth) {
                val startX = i * actualToothWidth
                val midX = startX + (actualToothWidth / 2f)
                val endX = startX + actualToothWidth
                path.lineTo(midX, heightPx)
                path.lineTo(endX, 0f)
            }
            path.close()
        }

        drawPath(path = path, color = color)
    }
}
