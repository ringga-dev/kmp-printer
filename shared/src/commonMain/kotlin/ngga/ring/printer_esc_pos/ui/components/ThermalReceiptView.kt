package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.preview.PreviewResult
import ngga.ring.printer_esc_pos.ui.theme.LocalThemeIsDark
import ngga.ring.printer_esc_pos.ui.theme.ThermalPaperBgDark
import ngga.ring.printer_esc_pos.ui.theme.ThermalPaperBgLight
import ngga.ring.printer_esc_pos.ui.theme.ThermalPaperInkDark
import ngga.ring.printer_esc_pos.ui.theme.ThermalPaperInkLight

@Composable
fun ThermalReceiptView(
    preview: PreviewResult,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThemeIsDark.current
    val paperBg = if (isDark) ThermalPaperBgDark else ThermalPaperBgLight
    val paperInk = if (isDark) ThermalPaperInkDark else ThermalPaperInkLight
    val borderColor = if (isDark) Color(0xFF2C3530) else Color(0xFFE2E6E2)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(8.dp)),
        color = paperBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Receipt header badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECEIPT PREVIEW (${preview.charsPerLine} CPL / ${preview.paperWidthDots} Dots)",
                    style = MaterialTheme.typography.labelSmall,
                    color = paperInk.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${preview.paperWidthMm}mm",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = paperInk.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = paperInk.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            if (preview.blocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No preview available yet.\nConfigure printer or trigger a print test.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = paperInk.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                preview.blocks.forEach { block ->
                    RenderPreviewBlock(block = block, paperInk = paperInk)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                color = paperInk.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            // Paper Cut Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✄ - - - - - - - - - - - - - - - - - - - - - - - ✄",
                    style = MaterialTheme.typography.labelSmall,
                    color = paperInk.copy(alpha = 0.35f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun RenderPreviewBlock(
    block: PreviewBlock,
    paperInk: Color
) {
    when (block) {
        is PreviewBlock.Text -> {
            val align = when (block.alignment) {
                TextAlignment.CENTER -> TextAlign.Center
                TextAlignment.RIGHT -> TextAlign.End
                TextAlignment.LEFT -> TextAlign.Start
            }
            val fontSize = (13 * block.widthMultiplier).coerceIn(11, 20).sp
            val lineHeight = (fontSize.value * 1.35f).sp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (block.isInverted) {
                            Modifier
                                .background(paperInk)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        } else Modifier
                    )
            ) {
                Text(
                    text = block.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = align,
                    color = if (block.isInverted) MaterialTheme.colorScheme.surface else paperInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (block.isUnderline) TextDecoration.Underline else TextDecoration.None
                )
            }
        }

        is PreviewBlock.KeyValue -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (block.isInverted) {
                            Modifier
                                .background(paperInk)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = block.key,
                    color = if (block.isInverted) MaterialTheme.colorScheme.surface else paperInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = block.value,
                    color = if (block.isInverted) MaterialTheme.colorScheme.surface else paperInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        is PreviewBlock.Divider -> {
            Text(
                text = block.char.toString().repeat(32),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = paperInk.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        is PreviewBlock.Space -> {
            Spacer(modifier = Modifier.height(10.dp))
        }

        is PreviewBlock.Barcode -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = when (block.alignment) {
                    TextAlignment.CENTER -> Alignment.CenterHorizontally
                    TextAlignment.RIGHT -> Alignment.End
                    TextAlignment.LEFT -> Alignment.Start
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewWeek,
                    contentDescription = "Barcode",
                    tint = paperInk,
                    modifier = Modifier.size(width = 140.dp, height = 40.dp)
                )
                Text(
                    text = block.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = paperInk.copy(alpha = 0.8f)
                )
            }
        }

        is PreviewBlock.QRCode -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = when (block.alignment) {
                    TextAlignment.CENTER -> Alignment.CenterHorizontally
                    TextAlignment.RIGHT -> Alignment.End
                    TextAlignment.LEFT -> Alignment.Start
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = "QR Code",
                    tint = paperInk,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = block.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = paperInk.copy(alpha = 0.7f)
                )
            }
        }

        is PreviewBlock.Image -> {
            val align = when (block.alignment) {
                TextAlignment.CENTER -> Alignment.CenterHorizontally
                TextAlignment.RIGHT -> Alignment.End
                TextAlignment.LEFT -> Alignment.Start
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = align
            ) {
                val previewBitmap = block.previewData as? ImageBitmap
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Receipt Image",
                        modifier = Modifier
                            .height(60.dp)
                            .shadow(1.dp)
                    )
                } else {
                    Surface(
                        color = paperInk.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "[IMAGE ${block.width}x${block.height}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = paperInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        is PreviewBlock.SystemCommand -> {
            Surface(
                color = paperInk.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(
                    text = "⚙ [${block.label}] ${block.detail}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = paperInk.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
