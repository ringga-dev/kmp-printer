package ngga.ring.printer

import ngga.ring.printer.model.*
import ngga.ring.printer.util.ConnectionState
import kotlinx.coroutines.flow.*
import ngga.ring.printer.manager.PrinterPermissionManager
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.repository.DefaultPrinterRepository
import ngga.ring.printer.repository.PrinterRepository
import ngga.ring.printer.usecase.DiscoverPrintersUseCase
import ngga.ring.printer.usecase.GetPrinterDiagnosticsUseCase
import ngga.ring.printer.usecase.PrintRawUseCase
import ngga.ring.printer.usecase.PrintReceiptUseCase
import ngga.ring.printer.usecase.PrintTestPageUseCase
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

/**
 * The "Satu Pintu" (Single Entry Point) for the printer library.
 * This class handles all printer operations using a unified Connector architecture.
 */
class KmpPrinter(
    val connectorFactory: PrinterConnectorFactory = PrinterConnectorFactory(),
    private val repository: PrinterRepository = DefaultPrinterRepository(connectorFactory)
) {

    /**
     * Platform-independent utility for managing printer-related permissions.
     */
    private val permissionManager = PrinterPermissionManager()

    val receiptService = ReceiptService()
    
    /**
     * Observe the current connection status of the printer.
     */
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val printRawUseCase = PrintRawUseCase(repository)
    private val printReceiptUseCase = PrintReceiptUseCase(printRawUseCase)
    private val printTestPageUseCase = PrintTestPageUseCase(receiptService, printRawUseCase)
    private val discoverPrintersUseCase = DiscoverPrintersUseCase(repository)
    private val diagnosticsUseCase = GetPrinterDiagnosticsUseCase()

    /**
     * Checks and requests the necessary permissions for discovery and printing.
     * @param type The connection type (e.g., "BLUETOOTH", "USB", "NETWORK").
     * @param onResult Callback with the final permission state.
     */
    fun checkAndRequestPermissions(type: String, onResult: (Boolean) -> Unit) {
        if (permissionManager.hasPermissions(type)) {
            onResult(true)
        } else {
            permissionManager.requestPermissions(type, onResult)
        }
    }

    /**
     * Creates a new CommandBuilder pre-configured for the specific printer.
     */
    fun newCommandBuilder(config: PrinterConfig): ESCPosCommandBuilder {
        return ESCPosCommandBuilder.fromPrinterConfig(config)
    }

    /**
     * Discovers printers based on the specified type.
     */
    fun discovery(
        type: String, 
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit = {}
    ): Flow<List<DiscoveredPrinter>> {
        return discoverPrintersUseCase(type, config, onLog)
    }

    fun platformReport(): PrinterPlatformReport {
        return diagnosticsUseCase.report()
    }

    fun troubleshootingHint(connectionType: String): String {
        return diagnosticsUseCase.troubleshootingHint(connectionType)
    }

    fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return diagnosticsUseCase.diagnoseUsb(config)
    }

    fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic {
        return diagnosticsUseCase.diagnoseBle(config)
    }

    fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        return diagnosticsUseCase.diagnoseSerial(config)
    }

    suspend fun testConnection(config: PrinterConfig): PrintStatus {
        return repository.testConnection(config)
    }

    /**
     * Prints a professionally styled receipt using the specified configuration and data.
     */
    fun printReceipt(
        config: PrinterConfig,
        data: ByteArray,
    ): Flow<PrintStatus> = printReceiptUseCase(config, data)

    /**
     * Sends raw ESC/POS bytes to the printer.
     * This is the lowest level call for custom printing logic.
     * 
     * @param config The target printer configuration.
     * @param data The raw byte array to send.
     */
    fun printRaw(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> = printRawUseCase(config, data)

    /**
     * Prints a professional hardware test page containing styles, barcodes, and QR codes.
     */
    fun printTestPage(config: PrinterConfig): Flow<PrintStatus> = printTestPageUseCase(config)

    /**
     * Prints using a DSL-style builder.
     * Handles connection and data sending automatically.
     */
    suspend fun print(
        config: PrinterConfig,
        block: ESCPosCommandBuilder.() -> Unit
    ): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val builder = newCommandBuilder(config).initialize()
        builder.block()
        
        printRaw(config, builder.build()).collect { status: PrintStatus ->
            emit(status)
        }
    }

    /**
     * Monitors the real-time status of the connected printer.
     * Emits PrinterStatus updates (online, paper out, cover open, etc.).
     *
     * @param config The printer configuration.
     * @param intervalMs Polling interval in milliseconds (default 2000ms).
     */
    fun monitorStatus(config: PrinterConfig, intervalMs: Long = 2000): Flow<PrinterStatus> {
        return repository.monitorStatus(config, intervalMs)
    }

    /**
     * Queries the printer status once.
     */
    suspend fun queryStatus(): PrinterStatus {
        return repository.queryStatus()
    }

    /**
     * Manually disconnects the current active connector.
     */
    suspend fun disconnect() {
        repository.disconnect()
    }
}
