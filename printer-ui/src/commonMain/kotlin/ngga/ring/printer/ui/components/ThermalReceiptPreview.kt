package ngga.ring.printer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.ui.theme.ReceiptPaperTheme
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.LineType
import ngga.ring.printer.util.preview.PreviewResult
import ngga.ring.printer.util.preview.VirtualLine

/**
 * Highly customizable and responsive thermal receipt preview component.
 * Renders ESC/POS preview lines with authentic typography, alignment, and paper decoration.
 */
@Composable
fun ThermalReceiptPreview(
    previewResult: PreviewResult,
    modifier: Modifier = Modifier,
    paperTheme: ReceiptPaperTheme = ReceiptPaperTheme.ClassicThermal,
    headerContent: @Composable (() -> Unit)? = null,
    footerContent: @Composable (() -> Unit)? = null,
    actionButtons: @Composable (RowScope.() -> Unit)? = null,
    customLineRenderer: @Composable ((VirtualLine) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val maxWidthDp = if (previewResult.paperWidthDots > 400) 420.dp else 320.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Paper Container
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = maxWidthDp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                shape = RoundedCornerShape(paperTheme.cornerRadius),
                colors = CardDefaults.cardColors(containerColor = paperTheme.paperColor),
                elevation = CardDefaults.cardElevation(defaultElevation = paperTheme.shadowElevation),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    if (paperTheme.showCutEdgeZigzag) {
                        PaperCutDecoration(color = paperTheme.dividerColor, isTop = true)
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        headerContent?.invoke()

                        previewResult.virtualLines.forEach { line ->
                            if (customLineRenderer != null) {
                                customLineRenderer(line)
                            } else {
                                DefaultLineRenderer(line = line, theme = paperTheme)
                            }
                        }

                        footerContent?.invoke()
                    }

                    if (paperTheme.showCutEdgeZigzag) {
                        PaperCutDecoration(color = paperTheme.dividerColor, isTop = false)
                    }
                }
            }
        }

        // Action Toolbar
        if (actionButtons != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = maxWidthDp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actionButtons()
            }
        }
    }
}

@Composable
private fun DefaultLineRenderer(
    line: VirtualLine,
    theme: ReceiptPaperTheme
) {
    val textAlign = when (line.alignment) {
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.RIGHT -> TextAlign.End
        else -> TextAlign.Start
    }

    val fontSize = (12 * line.heightMultiplier).coerceIn(10, 24).sp
    val fontWeight = if (line.isBold) FontWeight.Bold else FontWeight.Normal
    val textDecoration = if (line.isUnderline) TextDecoration.Underline else TextDecoration.None

    when (line.type) {
        LineType.SPACE -> {
            Spacer(modifier = Modifier.height(8.dp))
        }
        LineType.DIVIDER -> {
            HorizontalDivider(
                color = theme.dividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        LineType.BARCODE, LineType.QR_CODE -> {
            Surface(
                color = theme.dividerColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (line.type == LineType.BARCODE) "||||| BARCODE |||||" else "[ QR CODE ]",
                        fontFamily = theme.fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = line.content,
                        fontFamily = theme.fontFamily,
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        LineType.IMAGE -> {
            Surface(
                color = theme.dividerColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "🖼 ${line.content}",
                    fontFamily = theme.fontFamily,
                    color = theme.textColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
        else -> {
            val contentBoxModifier = if (line.isInverted) {
                Modifier
                    .background(theme.invertedBackgroundColor, RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = when (line.alignment) {
                    TextAlignment.CENTER -> Alignment.Center
                    TextAlignment.RIGHT -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
            ) {
                Text(
                    text = line.content,
                    fontFamily = theme.fontFamily,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    color = if (line.isInverted) theme.invertedTextColor else theme.textColor,
                    modifier = contentBoxModifier
                )
            }
        }
    }
}
