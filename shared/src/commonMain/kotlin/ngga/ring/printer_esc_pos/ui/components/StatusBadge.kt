package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer_esc_pos.ui.theme.StatusError
import ngga.ring.printer_esc_pos.ui.theme.StatusInfo
import ngga.ring.printer_esc_pos.ui.theme.StatusSuccess
import ngga.ring.printer_esc_pos.ui.theme.StatusWarning

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, dotColor, isAnimated) = when (connectionState) {
        is ConnectionState.Connected -> Triple("Connected", StatusSuccess, false)
        ConnectionState.Connecting -> Triple("Connecting…", StatusWarning, true)
        is ConnectionState.Error -> Triple("Error", StatusError, false)
        ConnectionState.Disconnected -> Triple("Disconnected", MaterialTheme.colorScheme.outline, false)
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PulsingDot(color = dotColor, isPulsing = isAnimated)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PrintStatusBanner(
    status: PrintStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status is PrintStatus.Idle) return

    val (bg, contentColor, icon, text) = when (status) {
        PrintStatus.Connecting -> Quadruple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Rounded.Sync,
            "Connecting to printer…"
        )
        PrintStatus.Processing -> Quadruple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Rounded.Print,
            "Processing print job…"
        )
        PrintStatus.Sending -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Rounded.Sync,
            "Sending data to printer…"
        )
        PrintStatus.Success -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Rounded.CheckCircle,
            "Print job completed successfully!"
        )
        is PrintStatus.Error -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Rounded.Error,
            "Print Failed: ${status.message}"
        )
        PrintStatus.Idle -> Quadruple(
            Color.Transparent,
            Color.Transparent,
            Icons.Rounded.Print,
            ""
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = bg,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PulsingDot(
    color: Color,
    isPulsing: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        animScale
    } else 1f

    Box(
        modifier = modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
