package ngga.ring.printer.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterDevice

/**
 * Headless controller for printer discovery.
 * Manages reactive scanning coroutines, active transport filter, and device list.
 */
@Stable
class PrinterDiscoveryState(
    private val printer: KmpPrinter,
    private val scope: CoroutineScope,
    initialTransport: PrinterConnection = PrinterConnection.BLUETOOTH
) {
    var selectedTransport by mutableStateOf(initialTransport)
        private set

    var devices by mutableStateOf<List<PrinterDevice>>(emptyList())
        private set

    var isScanning by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var scanJob: Job? = null

    init {
        startScan()
    }

    fun selectTransport(transport: PrinterConnection) {
        if (selectedTransport == transport) return
        selectedTransport = transport
        startScan()
    }

    fun startScan(config: DiscoveryConfig = DiscoveryConfig()) {
        scanJob?.cancel()
        isScanning = true
        errorMessage = null
        devices = emptyList()

        scanJob = scope.launch {
            printer.discoverDevices(selectedTransport, config)
                .catch { e ->
                    errorMessage = e.message ?: "Failed to scan printers"
                    isScanning = false
                }
                .collect { foundDevices ->
                    devices = foundDevices
                    isScanning = false
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        isScanning = false
    }
}

/**
 * Remembers and creates an instance of [PrinterDiscoveryState].
 */
@Composable
fun rememberPrinterDiscoveryState(
    printer: KmpPrinter,
    initialTransport: PrinterConnection = PrinterConnection.BLUETOOTH
): PrinterDiscoveryState {
    val scope = rememberCoroutineScope()
    return remember(printer, initialTransport) {
        PrinterDiscoveryState(printer, scope, initialTransport)
    }
}
