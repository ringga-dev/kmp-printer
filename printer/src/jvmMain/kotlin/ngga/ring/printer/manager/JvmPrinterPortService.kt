package ngga.ring.printer.manager

import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConnectionType

data class JvmSerialPortInfo(
    val name: String,
    val address: String,
    val descriptiveName: String,
    val looksLikePrinter: Boolean,
    val looksLikeBluetooth: Boolean
)

/**
 * JVM port inventory service.
 *
 * JVM desktop hardware support is OS-dependent, so this class centralizes
 * serial-port discovery and classification instead of scattering heuristics in
 * the connector factory.
 */
class JvmPrinterPortService {
    private val queueService = JvmPrintQueueService()
    private val usbService = JvmUsbDeviceService()
    private val bleService = JvmBleService()
    private val backend: JvmOsPrinterBackend = when (JvmOperatingSystem.current()) {
        JvmOperatingSystem.WINDOWS -> WindowsJvmPrinterBackend()
        JvmOperatingSystem.LINUX -> LinuxJvmPrinterBackend()
        JvmOperatingSystem.MACOS -> MacosJvmPrinterBackend()
        JvmOperatingSystem.OTHER -> GenericJvmPrinterBackend()
    }

    fun currentOs(): JvmOperatingSystem = backend.os

    fun connectionHint(type: String): String = backend.connectionHint(type)

    fun listSerialPorts(): List<JvmSerialPortInfo> = backend.listSerialPorts()

    fun listPrintQueues(): List<JvmPrintQueueInfo> = queueService.listQueues()

    fun rawUsbHint(): String = usbService.troubleshootingHint()

    fun bleHint(): String = bleService.troubleshootingHint()

    fun discoverRawUsbPrinters(): List<DiscoveredPrinter> = usbService.discoverRawUsbPrinters()

    fun discoverSerialBackedPrinters(type: String): List<DiscoveredPrinter> {
        val normalizedType = PrinterConnectionType.normalize(type)
        return listSerialPorts()
            .filter { port ->
                when (normalizedType) {
                    PrinterConnectionType.SERIAL -> true
                    PrinterConnectionType.USB -> true
                    PrinterConnectionType.BLUETOOTH -> true
                    PrinterConnectionType.BLUETOOTH_LE -> port.looksLikeBluetooth
                    else -> port.looksLikePrinter
                }
            }
            .map { port ->
                DiscoveredPrinter(
                    name = port.name,
                    connectionType = normalizedType,
                    address = port.address
                )
            }
    }

    fun discoverPrintQueuePrinters(type: String): List<DiscoveredPrinter> {
        val normalizedType = PrinterConnectionType.normalize(type)
        return listPrintQueues()
            .filter { queue ->
                when (normalizedType) {
                    PrinterConnectionType.USB -> queue.looksLikeUsb || queue.looksLikePrinter
                    PrinterConnectionType.BLUETOOTH -> queue.looksLikeBluetooth || queue.looksLikePrinter
                    else -> queue.looksLikePrinter
                }
            }
            .map { queue ->
                DiscoveredPrinter(
                    name = queue.name,
                    connectionType = normalizedType,
                    address = queue.name
                )
            }
    }
}
