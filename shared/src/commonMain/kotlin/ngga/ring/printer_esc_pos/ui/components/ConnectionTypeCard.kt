package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConnectionTypeOption(
    val type: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

val connectionTypeOptions = listOf(
    ConnectionTypeOption("NETWORK", "Network", Icons.Filled.Lan, Color(0xFF1976D2)),
    ConnectionTypeOption("BLUETOOTH", "Bluetooth", Icons.Filled.Bluetooth, Color(0xFF455A64)),
    ConnectionTypeOption("BLUETOOTH_LE", "BLE", Icons.Filled.BluetoothConnected, Color(0xFF00897B)),
    ConnectionTypeOption("USB", "USB", Icons.Filled.Usb, Color(0xFF388E3C)),
    ConnectionTypeOption("SERIAL", "Serial", Icons.Filled.Cable, Color(0xFFF57C00)),
    ConnectionTypeOption("VIRTUAL", "Virtual", Icons.Filled.Computer, Color(0xFF757575))
)

@Composable
fun ConnectionTypeGrid(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunked = connectionTypeOptions.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { option ->
                    val isSelected = selectedType == option.type
                    Card(
                        onClick = { onTypeSelected(option.type) },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) option.color.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.title,
                                modifier = Modifier.size(28.dp),
                                tint = if (isSelected) option.color
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) option.color
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f).width(1.dp))
                }
            }
        }
    }
}
