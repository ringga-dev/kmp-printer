package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer_esc_pos.ui.components.GlassCard
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(viewModel: PrinterViewModel) {
    val devices by viewModel.discoveredPrinters.collectAsState()
    val log by viewModel.discoveryLog.collectAsState()
    val mode by viewModel.discoveryMode.collectAsState()
    val showVirtual by viewModel.showVirtual.collectAsState()
    val config by viewModel.config.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "Discovery",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Hardware synchronization engine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Mode Selector Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = mutableListOf("BLUETOOTH", "USB", "NETWORK")
                if (showVirtual) modes.add("VIRTUAL")
                
                modes.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { viewModel.setDiscoveryMode(m) },
                        label = { Text(m, fontSize = 11.sp) },
                        leadingIcon = {
                            if (mode == m) Icon(Icons.Default.Check, null, Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // Status & Log
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            val isScanning = log.contains("Scanning", ignoreCase = true) || log.contains("sent", ignoreCase = true)
            if (isScanning) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Radar, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(log, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(devices) { device ->
                DiscoveryCard(
                    device = device,
                    isSelected = config.address == device.address,
                    onClick = { viewModel.selectPrinter(device) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryCard(device: DiscoveredPrinter, isSelected: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ) {
                Icon(
                    imageVector = when(device.connectionType) {
                        "NETWORK" -> Icons.Default.Lan
                        "USB" -> Icons.Default.Usb
                        "VIRTUAL" -> Icons.Default.Computer
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address ?: "Auto-assigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
