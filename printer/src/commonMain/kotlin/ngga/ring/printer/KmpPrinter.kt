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
import ngga.ring.printer.util.PrinterLogEvent
import ngga.ring.printer.util.PrinterLogger
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

    fun setLogger(logger: ((PrinterLogEvent) -> Unit)?) {
        PrinterLogger.setSink(logger)
    }

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

    fun checkAndRequestPermissions(type: PrinterConnection, onResult: (Boolean) -> Unit) {
        checkAndRequestPermissions(type.value, onResult)
    }

    /**
     * Creates a new CommandBuilder pre-configured for the specific printer.
     */
    fun newCommandBuilder(config: PrinterConfig): ESCPosCommandBuilder {
        return ESCPosCommandBuilder.fromPrinterConfig(config)
    }

    fun newCommandBuilder(config: PrinterTransportConfig): ESCPosCommandBuilder {
        return newCommandBuilder(config.toPrinterConfig())
    }

    fun newCommandBuilder(device: PrinterDevice, profile: PrinterProfile = PrinterProfile.MM58): ESCPosCommandBuilder {
        return newCommandBuilder(device.toPrinterConfig(profile))
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

    fun discovery(
        type: PrinterConnection,
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit = {}
    ): Flow<List<DiscoveredPrinter>> {
        return discovery(type.value, config, onLog)
    }

    fun platformReport(): PrinterPlatformReport {
        return diagnosticsUseCase.report()
    }

    fun troubleshootingHint(connectionType: String): String {
        return diagnosticsUseCase.troubleshootingHint(connectionType)
    }

    fun troubleshootingHint(connectionType: PrinterConnection): String {
        return troubleshootingHint(connectionType.value)
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

    suspend fun testConnection(device: PrinterDevice, profile: PrinterProfile = PrinterProfile.MM58): PrintStatus {
        return testConnection(device.toPrinterConfig(profile))
    }

    /**
     * Prints a professionally styled receipt using the specified configuration and data.
     */
    fun printReceipt(
        config: PrinterConfig,
        data: ByteArray,
    ): Flow<PrintStatus> = printReceiptUseCase(config, data)

    fun printReceipt(
        device: PrinterDevice,
        data: ByteArray,
        profile: PrinterProfile = PrinterProfile.MM58
    ): Flow<PrintStatus> = printReceipt(device.toPrinterConfig(profile), data)

    /**
     * Sends raw ESC/POS bytes to the printer.
     * This is the lowest level call for custom printing logic.
     * 
     * @param config The target printer configuration.
     * @param data The raw byte array to send.
     */
    fun printRaw(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> = printRawUseCase(config, data)

    fun printRaw(config: PrinterTransportConfig, data: ByteArray): Flow<PrintStatus> {
        return printRaw(config.toPrinterConfig(), data)
    }

    fun printRaw(device: PrinterDevice, data: ByteArray, profile: PrinterProfile = PrinterProfile.MM58): Flow<PrintStatus> {
        return printRaw(device.toPrinterConfig(profile), data)
    }

    /**
     * Prints a professional hardware test page containing styles, barcodes, and QR codes.
     */
    fun printTestPage(config: PrinterConfig): Flow<PrintStatus> = printTestPageUseCase(config)

    fun printTestPage(device: PrinterDevice, profile: PrinterProfile = PrinterProfile.MM58): Flow<PrintStatus> {
        return printTestPage(device.toPrinterConfig(profile))
    }

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

    suspend fun print(
        config: PrinterTransportConfig,
        block: ESCPosCommandBuilder.() -> Unit
    ): Flow<PrintStatus> {
        return print(config.toPrinterConfig(), block)
    }

    suspend fun print(
        device: PrinterDevice,
        profile: PrinterProfile = PrinterProfile.MM58,
        block: ESCPosCommandBuilder.() -> Unit
    ): Flow<PrintStatus> {
        return print(device.toPrinterConfig(profile), block)
    }

    /**
     * Prints a label using TSPL command builder DSL.
     */
    suspend fun printLabel(
        config: PrinterConfig,
        widthMm: Double = 40.0,
        heightMm: Double = 30.0,
        block: ngga.ring.printer.util.tspl.TsplCommandBuilder.() -> Unit
    ): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val builder = ngga.ring.printer.util.tspl.TsplCommandBuilder(widthMm, heightMm)
        builder.block()
        printRaw(config, builder.build()).collect { status ->
            emit(status)
        }
    }

    suspend fun printLabel(
        device: PrinterDevice,
        widthMm: Double = 40.0,
        heightMm: Double = 30.0,
        profile: PrinterProfile = PrinterProfile.MM58,
        block: ngga.ring.printer.util.tspl.TsplCommandBuilder.() -> Unit
    ): Flow<PrintStatus> {
        return printLabel(device.toPrinterConfig(profile), widthMm, heightMm, block)
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

    fun monitorStatus(device: PrinterDevice, profile: PrinterProfile = PrinterProfile.MM58, intervalMs: Long = 2000): Flow<PrinterStatus> {
        return monitorStatus(device.toPrinterConfig(profile), intervalMs)
    }

    /**
     * Queries the printer status once.
     */
    suspend fun queryStatus(): PrinterStatus {
        return repository.queryStatus()
    }

    /**
     * Creates an isolated, dedicated [PrinterSession] for this config.
     * Useful when managing multiple concurrent printers (e.g. Kitchen and Cashier).
     */
    fun openSession(config: PrinterConfig): ngga.ring.printer.session.PrinterSession {
        val connector = connectorFactory.create(config)
        return ngga.ring.printer.session.DefaultPrinterSession(config, connector)
    }

    fun openSession(device: PrinterDevice, profile: PrinterProfile = PrinterProfile.MM58): ngga.ring.printer.session.PrinterSession {
        return openSession(device.toPrinterConfig(profile))
    }

    fun openSession(config: PrinterTransportConfig): ngga.ring.printer.session.PrinterSession {
        return openSession(config.toPrinterConfig())
    }

    /**
     * Manually disconnects the current active connector.
     */
    suspend fun disconnect() {
        repository.disconnect()
    }
}
