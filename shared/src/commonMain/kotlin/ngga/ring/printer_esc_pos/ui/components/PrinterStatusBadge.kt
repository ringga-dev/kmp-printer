package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.model.PrintStatus

@Composable
fun PrinterStatusBadge(
    connectionState: ConnectionState,
    printStatus: PrintStatus,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when {
        connectionState is ConnectionState.Connected -> Triple(
            Icons.Filled.CheckCircle, "Connected", Color(0xFF2E7D32)
        )
        connectionState is ConnectionState.Connecting -> Triple(
            Icons.Filled.Link, "Connecting...", Color(0xFFF57F17)
        )
        connectionState is ConnectionState.Disconnected -> Triple(
            Icons.Filled.LinkOff, "Disconnected", Color(0xFF616161)
        )
        printStatus is PrintStatus.Processing || printStatus is PrintStatus.Sending -> Triple(
            Icons.Filled.Print, "Printing...", Color(0xFF1565C0)
        )
        printStatus is PrintStatus.Error -> Triple(
            Icons.Filled.Error, "Error: ${printStatus.message}", Color(0xFFC62828)
        )
        printStatus is PrintStatus.Success -> Triple(
            Icons.Filled.CheckCircle, "Print Success", Color(0xFF2E7D32)
        )
        else -> Triple(
            Icons.Filled.HourglassEmpty, "Idle", Color(0xFF757575)
        )
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1
            )
        }
    }
}
