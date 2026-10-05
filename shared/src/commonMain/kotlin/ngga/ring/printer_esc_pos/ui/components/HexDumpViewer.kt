package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer_esc_pos.ui.theme.LocalThemeIsDark

@Composable
fun HexDumpViewer(
    hexString: String,
    totalBytes: Int,
    modifier: Modifier = Modifier,
    onCopied: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = LocalThemeIsDark.current
    val bg = if (isDark) Color(0xFF141916) else Color(0xFF1E2622)
    val textCode = Color(0xFF7CE4B5)
    val textDim = Color(0xFF8C9B93)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = textCode,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "RAW ESC/POS BYTECODE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textCode,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "($totalBytes bytes)",
                        style = MaterialTheme.typography.labelSmall,
                        color = textDim,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (hexString.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(hexString))
                            onCopied?.invoke()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = textCode
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Hex",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = hexString.ifBlank { "No bytecode generated. Click 'Inspect Hex' to dump current command." },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = if (hexString.isBlank()) textDim else textCode
                )
            }
        }
    }
}
