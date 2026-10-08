package ngga.ring.printer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterDevice
import ngga.ring.printer.ui.adaptive.AdaptiveBox
import ngga.ring.printer.ui.state.rememberPrinterDiscoveryState

/**
 * Responsive printer discovery dialog/sheet with filter tabs, scanner animations, and Slot API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterDiscoverySheet(
    printer: KmpPrinter,
    onDismiss: () -> Unit,
    onDeviceSelected: (PrinterDevice) -> Unit,
    modifier: Modifier = Modifier,
    initialConnection: PrinterConnection = PrinterConnection.BLUETOOTH,
    availableTransports: List<PrinterConnection> = listOf(
        PrinterConnection.BLUETOOTH,
        PrinterConnection.BLE,
        PrinterConnection.USB,
        PrinterConnection.NETWORK
    ),
    titleContent: @Composable (() -> Unit)? = null,
    emptyStateContent: @Composable (() -> Unit)? = null,
    itemCardContent: @Composable ((device: PrinterDevice, isSelected: Boolean, onSelect: () -> Unit) -> Unit)? = null,
    onTestPrintClick: ((PrinterDevice) -> Unit)? = null
) {
    val state = rememberPrinterDiscoveryState(printer, initialConnection)
    var selectedDevice by remember { mutableStateOf<PrinterDevice?>(null) }

    AdaptiveBox { windowSize ->
        if (windowSize.isCompact) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DiscoveryContent(
                    state = state,
                    availableTransports = availableTransports,
                    selectedDevice = selectedDevice,
                    onDeviceSelected = {
                        selectedDevice = it
                        onDeviceSelected(it)
                    },
                    onDismiss = onDismiss,
                    titleContent = titleContent,
                    emptyStateContent = emptyStateContent,
                    itemCardContent = itemCardContent,
                    onTestPrintClick = onTestPrintClick,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        } else {
            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    DiscoveryContent(
                        state = state,
                        availableTransports = availableTransports,
                        selectedDevice = selectedDevice,
                        onDeviceSelected = {
                            selectedDevice = it
                            onDeviceSelected(it)
                        },
                        onDismiss = onDismiss,
                        titleContent = titleContent,
                        emptyStateContent = emptyStateContent,
                        itemCardContent = itemCardContent,
                        onTestPrintClick = onTestPrintClick,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryContent(
    state: ngga.ring.printer.ui.state.PrinterDiscoveryState,
    availableTransports: List<PrinterConnection>,
    selectedDevice: PrinterDevice?,
    onDeviceSelected: (PrinterDevice) -> Unit,
    onDismiss: () -> Unit,
    titleContent: @Composable (() -> Unit)?,
    emptyStateContent: @Composable (() -> Unit)?,
    itemCardContent: @Composable ((device: PrinterDevice, isSelected: Boolean, onSelect: () -> Unit) -> Unit)?,
    onTestPrintClick: ((PrinterDevice) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = "Cari Printer",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state.startScan() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Scan Ulang"
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup"
                    )
                }
            }
        }

        // Transport Filter Tabs
        if (availableTransports.size > 1) {
            val selectedIndex = availableTransports.indexOf(state.selectedTransport).coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 16.dp,
                divider = {}
            ) {
                availableTransports.forEachIndexed { index, transport ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { state.selectTransport(transport) },
                        text = { Text(transport.value) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Device List or Loading
        if (state.isScanning && state.devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text(
                        text = "Memindai perangkat ${state.selectedTransport.value}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.devices.isEmpty()) {
            if (emptyStateContent != null) {
                emptyStateContent()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada printer ${state.selectedTransport.value} yang ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.devices) { device ->
                    val isSelected = selectedDevice?.address == device.address
                    if (itemCardContent != null) {
                        itemCardContent(device, isSelected) {
                            onDeviceSelected(device)
                        }
                    } else {
                        PrinterDeviceCard(
                            device = device,
                            isSelected = isSelected,
                            onSelect = { onDeviceSelected(device) },
                            onTestClick = if (onTestPrintClick != null) {
                                { onTestPrintClick(device) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
