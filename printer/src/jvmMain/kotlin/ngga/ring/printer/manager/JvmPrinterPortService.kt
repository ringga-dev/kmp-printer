package ngga.ring.printer.manager

import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterSerialFailureReason
import ngga.ring.printer.model.PrinterSerialPortDiagnostic
import com.fazecast.jSerialComm.SerialPort

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

    fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        val ports = listSerialPorts()
        val diagnostics = ports.map { it.toDiagnostic(config.connectionType) }
        val address = config.address

        if (address.isNullOrBlank()) {
            return PrinterSerialDiagnostic(
                portFound = false,
                canOpen = false,
                canWrite = false,
                failureReason = PrinterSerialFailureReason.INVALID_ADDRESS,
                message = "Serial/Bluetooth Classic address is empty.",
                suggestedFix = serialFix(config.connectionType, PrinterSerialFailureReason.INVALID_ADDRESS),
                ports = diagnostics
            )
        }

        val selected = ports.firstOrNull {
            it.address.equals(address, ignoreCase = true) ||
                it.name.equals(address, ignoreCase = true)
        }

        if (selected == null) {
            return PrinterSerialDiagnostic(
                portFound = false,
                canOpen = false,
                canWrite = false,
                failureReason = PrinterSerialFailureReason.PORT_NOT_FOUND,
                message = "Port $address was not found.",
                suggestedFix = serialFix(config.connectionType, PrinterSerialFailureReason.PORT_NOT_FOUND),
                ports = diagnostics
            )
        }

        val port = SerialPort.getCommPort(selected.address)
        port.baudRate = config.baudRate
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
            config.readTimeoutMs,
            config.connectionTimeoutMs
        )

        val opened = try {
            port.openPort()
        } catch (e: SecurityException) {
            return serialFailure(
                reason = PrinterSerialFailureReason.PERMISSION_DENIED,
                message = "Permission denied while opening ${selected.address}: ${e.message}",
                config = config,
                ports = diagnostics
            )
        } catch (e: Exception) {
            return serialFailure(
                reason = classifySerialException(e),
                message = "Failed to open ${selected.address}: ${e.message}",
                config = config,
                ports = diagnostics
            )
        }

        if (!opened) {
            return serialFailure(
                reason = PrinterSerialFailureReason.OPEN_FAILED,
                message = "Port ${selected.address} could not be opened.",
                config = config,
                ports = diagnostics
            )
        }

        return try {
            val out = port.outputStream
            out.write(byteArrayOf(0x1B, 0x40))
            out.flush()
            PrinterSerialDiagnostic(
                portFound = true,
                canOpen = true,
                canWrite = true,
                message = "Serial/Bluetooth Classic write probe succeeded on ${selected.address}.",
                ports = diagnostics
            )
        } catch (e: Exception) {
            serialFailure(
                reason = classifySerialException(e, fallback = PrinterSerialFailureReason.WRITE_FAILED),
                message = "Port ${selected.address} opened, but write probe failed: ${e.message}",
                config = config,
                ports = diagnostics,
                canOpen = true
            )
        } finally {
            try {
                port.closePort()
            } catch (_: Exception) {
            }
        }
    }

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

    private fun JvmSerialPortInfo.toDiagnostic(type: String): PrinterSerialPortDiagnostic {
        val normalizedType = PrinterConnectionType.normalize(type)
        val confidence = buildList {
            if (looksLikePrinter) add(45)
            if (looksLikeBluetooth && normalizedType == PrinterConnectionType.BLUETOOTH) add(35)
            if (address.contains("COM", ignoreCase = true)) add(10)
            if (address.contains("rfcomm", ignoreCase = true)) add(25)
            if (address.contains("tty", ignoreCase = true)) add(10)
        }.sum().coerceIn(0, 100)

        return PrinterSerialPortDiagnostic(
            name = name,
            address = address,
            isBluetoothLike = looksLikeBluetooth,
            isPrinterLike = looksLikePrinter,
            confidence = confidence,
            notes = buildList {
                if (looksLikeBluetooth) add("Bluetooth-like serial port")
                if (looksLikePrinter) add("Printer-like name")
            }
        )
    }

    private fun serialFailure(
        reason: PrinterSerialFailureReason,
        message: String,
        config: PrinterConfig,
        ports: List<PrinterSerialPortDiagnostic>,
        canOpen: Boolean = false
    ): PrinterSerialDiagnostic {
        return PrinterSerialDiagnostic(
            portFound = reason != PrinterSerialFailureReason.PORT_NOT_FOUND,
            canOpen = canOpen,
            canWrite = false,
            failureReason = reason,
            message = message,
            suggestedFix = serialFix(config.connectionType, reason),
            ports = ports
        )
    }

    private fun classifySerialException(
        error: Exception,
        fallback: PrinterSerialFailureReason = PrinterSerialFailureReason.OPEN_FAILED
    ): PrinterSerialFailureReason {
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("access") || message.contains("permission") -> PrinterSerialFailureReason.PERMISSION_DENIED
            message.contains("busy") || message.contains("denied") || message.contains("in use") -> PrinterSerialFailureReason.PORT_BUSY
            else -> fallback
        }
    }

    private fun serialFix(type: String, reason: PrinterSerialFailureReason): String {
        val normalizedType = PrinterConnectionType.normalize(type)
        return when (currentOs()) {
            JvmOperatingSystem.WINDOWS -> when (reason) {
                PrinterSerialFailureReason.PORT_NOT_FOUND -> "Pair the printer in Windows Bluetooth settings, then check the outgoing COM port and use that COMx value."
                PrinterSerialFailureReason.PORT_BUSY -> "Close other POS/printer apps using the COM port, then retry."
                PrinterSerialFailureReason.PERMISSION_DENIED -> "Run the app with permission to access the COM port or choose the correct outgoing COM port."
                else -> if (normalizedType == PrinterConnectionType.BLUETOOTH) "Use the outgoing Bluetooth COM port, not the incoming COM port." else connectionHint(type)
            }
            JvmOperatingSystem.LINUX -> when (reason) {
                PrinterSerialFailureReason.PORT_NOT_FOUND -> "Pair the printer and bind it with rfcomm, for example `sudo rfcomm bind /dev/rfcomm0 <MAC>`."
                PrinterSerialFailureReason.PERMISSION_DENIED -> "Add the user to dialout/uucp or adjust permissions for /dev/tty* or /dev/rfcomm*."
                PrinterSerialFailureReason.PORT_BUSY -> "Release the rfcomm/tty device from other processes, then retry."
                else -> connectionHint(type)
            }
            JvmOperatingSystem.MACOS -> when (reason) {
                PrinterSerialFailureReason.PORT_NOT_FOUND -> "Check whether macOS created a /dev/cu.* Bluetooth serial device; otherwise use printer queue or USB/network."
                PrinterSerialFailureReason.PORT_BUSY -> "Close other apps using the /dev/cu.* device, then retry."
                else -> connectionHint(type)
            }
            JvmOperatingSystem.OTHER -> connectionHint(type)
        }
    }
}
