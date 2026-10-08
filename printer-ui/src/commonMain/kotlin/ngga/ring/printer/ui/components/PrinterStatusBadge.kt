package ngga.ring.printer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState

/**
 * Responsive status badge showing connection state and hardware errors (paper out, cover open).
 */
@Composable
fun PrinterStatusBadge(
    connectionState: ConnectionState,
    printerStatus: PrinterStatus? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (statusText, statusColor) = when {
        printerStatus?.isPaperOut == true -> "Paper Out" to Color(0xFFEF4444)
        printerStatus?.isCoverOpen == true -> "Cover Open" to Color(0xFFF59E0B)
        connectionState is ConnectionState.Connected -> "Connected" to Color(0xFF10B981)
        connectionState is ConnectionState.Connecting -> "Connecting" to Color(0xFF3B82F6)
        connectionState is ConnectionState.Error -> "Error" to Color(0xFFEF4444)
        else -> "Disconnected" to Color(0xFF6B7280)
    }

    val animatedColor by animateColorAsState(targetValue = statusColor)

    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = animatedColor.copy(alpha = 0.12f),
        contentColor = animatedColor,
        modifier = clickableModifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
