package ngga.ring.printer.manager

import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterSerialFailureReason
import ngga.ring.printer.model.PrinterSerialPortDiagnostic
import com.fazecast.jSerialComm.SerialPort
import java.util.concurrent.TimeUnit

data class JvmSerialPortInfo(
    val name: String,
    val address: String,
    val descriptiveName: String,
    val looksLikePrinter: Boolean,
    val looksLikeBluetooth: Boolean
)

internal fun JvmSerialPortInfo.scoreBluetoothClassic(os: JvmOperatingSystem): Int {
    val normalizedAddress = address.lowercase()
    val normalizedName = name.lowercase()
    return buildList {
        if (looksLikeBluetooth) add(45)
        if (looksLikePrinter) add(20)
        when (os) {
            JvmOperatingSystem.WINDOWS -> {
                if (normalizedAddress.startsWith("com")) add(25)
                if (normalizedName.contains("bluetooth")) add(20)
                if (normalizedAddress.contains("bth")) add(10)
            }
            JvmOperatingSystem.LINUX -> {
                if (normalizedAddress.contains("rfcomm")) add(30)
                if (normalizedAddress.contains("tty")) add(15)
                if (normalizedName.contains("bluez")) add(10)
            }
            JvmOperatingSystem.MACOS -> {
                if (normalizedAddress.contains("/dev/cu.")) add(30)
                if (normalizedAddress.contains("/dev/tty.")) add(20)
                if (normalizedName.contains("bluetooth")) add(15)
            }
            JvmOperatingSystem.OTHER -> {
                if (normalizedAddress.contains("tty")) add(10)
                if (normalizedName.contains("bluetooth")) add(10)
            }
        }
    }.sum().coerceIn(0, 100)
}

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
            val environmentNote = bluetoothEnvironmentNote(normalizedType = config.connectionType, ports = diagnostics)
            return PrinterSerialDiagnostic(
                portFound = false,
                canOpen = false,
                canWrite = false,
                failureReason = PrinterSerialFailureReason.PORT_NOT_FOUND,
                message = buildString {
                    append("Port $address was not found.")
                    if (environmentNote.isNotBlank()) {
                        append(" ")
                        append(environmentNote)
                    }
                },
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
        val ports = listSerialPorts()
        val orderedPorts = if (normalizedType == PrinterConnectionType.BLUETOOTH) {
            ports.sortedByDescending { it.scoreBluetoothClassic(currentOs()) }
        } else {
            ports
        }

        return orderedPorts
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
        val queues = listPrintQueues()
        val orderedQueues = if (normalizedType == PrinterConnectionType.BLUETOOTH) {
            queues.sortedByDescending { it.scoreBluetoothClassic(currentOs()) }
        } else {
            queues
        }

        return orderedQueues
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
        val confidence = if (normalizedType == PrinterConnectionType.BLUETOOTH) {
            scoreBluetoothClassic(currentOs())
        } else {
            buildList {
                if (looksLikePrinter) add(45)
                if (looksLikeBluetooth && normalizedType == PrinterConnectionType.BLUETOOTH_LE) add(35)
                if (address.contains("COM", ignoreCase = true)) add(10)
                if (address.contains("rfcomm", ignoreCase = true)) add(25)
                if (address.contains("tty", ignoreCase = true)) add(10)
            }.sum().coerceIn(0, 100)
        }

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
                PrinterSerialFailureReason.PORT_NOT_FOUND -> if (normalizedType == PrinterConnectionType.BLUETOOTH) {
                    "Pair the printer in Windows Bluetooth settings, then use the outgoing COM port that Windows creates for the printer."
                } else {
                    "Check Device Manager for the correct COMx device and select the port that matches the printer."
                }
                PrinterSerialFailureReason.PORT_BUSY -> "Close other POS/printer apps using the COM port, then retry."
                PrinterSerialFailureReason.PERMISSION_DENIED -> "Run the app with permission to access the COM port or choose the correct outgoing COM port."
                else -> if (normalizedType == PrinterConnectionType.BLUETOOTH) "Use the outgoing Bluetooth COM port, not the incoming COM port." else connectionHint(type)
            }
            JvmOperatingSystem.LINUX -> when (reason) {
                PrinterSerialFailureReason.PORT_NOT_FOUND -> if (normalizedType == PrinterConnectionType.BLUETOOTH) {
                    "Pair the printer first, then bind it with rfcomm so BlueZ exposes /dev/rfcomm*; for example `sudo rfcomm bind /dev/rfcomm0 <MAC>`."
                } else {
                    "Check whether the printer is exposed as /dev/ttyUSB*, /dev/ttyACM*, or /dev/rfcomm* and bind rfcomm if needed."
                }
                PrinterSerialFailureReason.PERMISSION_DENIED -> "Add the user to dialout/uucp or adjust permissions for /dev/tty* or /dev/rfcomm*."
                PrinterSerialFailureReason.PORT_BUSY -> "Release the rfcomm/tty device from other processes, then retry."
                else -> connectionHint(type)
            }
            JvmOperatingSystem.MACOS -> when (reason) {
                PrinterSerialFailureReason.PORT_NOT_FOUND -> if (normalizedType == PrinterConnectionType.BLUETOOTH) {
                    "Check whether macOS created an outgoing /dev/cu.* Bluetooth device for the paired printer; otherwise use printer queue or USB/network."
                } else {
                    "Check whether macOS exposed the printer as /dev/cu.* or /dev/tty.*; otherwise use printer queue or USB/network."
                }
                PrinterSerialFailureReason.PORT_BUSY -> "Close other apps using the /dev/cu.* device, then retry."
                else -> connectionHint(type)
            }
            JvmOperatingSystem.OTHER -> connectionHint(type)
        }
    }

    private fun bluetoothEnvironmentNote(
        normalizedType: String,
        ports: List<PrinterSerialPortDiagnostic>
    ): String {
        if (PrinterConnectionType.normalize(normalizedType) != PrinterConnectionType.BLUETOOTH) {
            return ""
        }

        val statusNotes = buildList {
            bluetoothAdapterStatusNote().takeIf { it.isNotBlank() }?.let { add(it) }
            bluetoothPairingStatusNote().takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val topCandidates = ports
            .sortedByDescending { it.confidence }
            .take(3)
            .filter { it.confidence > 0 }

        val portNote = when {
            ports.isEmpty() -> "No serial ports were exposed by the JVM, so Bluetooth Classic is likely not paired or the OS is not exposing an outgoing port yet."
            topCandidates.isEmpty() -> "The JVM can see serial ports, but none look like a Bluetooth Classic device."
            else -> "Closest candidates: ${topCandidates.joinToString { "${it.address} (${it.confidence}%)" }}."
        }

        return buildString {
            append(portNote)
            if (statusNotes.isNotEmpty()) {
                append(" ")
                append(statusNotes.joinToString(" "))
            }
        }
    }

    private fun bluetoothAdapterStatusNote(): String {
        return when (currentOs()) {
            JvmOperatingSystem.WINDOWS -> {
                val service = runProbe(
                    listOf("powershell", "-NoProfile", "-Command", "(Get-Service bthserv -ErrorAction SilentlyContinue).Status"),
                    timeoutMs = 2000
                )
                when {
                    !service.available -> "Windows Bluetooth service status could not be queried."
                    service.output.contains("Running", ignoreCase = true) -> "Windows Bluetooth Support Service is running."
                    service.output.isNotBlank() -> "Windows Bluetooth Support Service is not running."
                    else -> "Windows Bluetooth service status is unknown."
                }
            }
            JvmOperatingSystem.LINUX -> {
                val show = runProbe(listOf("bluetoothctl", "show"), timeoutMs = 2000)
                when {
                    !show.available -> "bluetoothctl is not available, so adapter status could not be inspected."
                    show.output.contains("No default controller", ignoreCase = true) -> "BlueZ does not see a Bluetooth controller."
                    show.output.contains("Powered: no", ignoreCase = true) -> "BlueZ sees a controller, but it is powered off."
                    show.output.contains("Powered: yes", ignoreCase = true) -> "BlueZ sees a powered Bluetooth controller."
                    else -> "BlueZ adapter status is available but not definitive."
                }
            }
            JvmOperatingSystem.MACOS -> {
                val profiler = runProbe(listOf("system_profiler", "SPBluetoothDataType"), timeoutMs = 2500)
                when {
                    !profiler.available -> "macOS Bluetooth adapter status could not be queried."
                    profiler.output.contains("Bluetooth Power: Off", ignoreCase = true) -> "macOS Bluetooth is powered off."
                    profiler.output.contains("No information found", ignoreCase = true) -> "macOS did not report a Bluetooth adapter."
                    else -> "macOS Bluetooth adapter appears available."
                }
            }
            JvmOperatingSystem.OTHER -> ""
        }
    }

    private fun bluetoothPairingStatusNote(): String {
        return when (currentOs()) {
            JvmOperatingSystem.WINDOWS -> {
                val ports = listSerialPorts()
                if (ports.any { it.looksLikeBluetooth || it.address.startsWith("COM", ignoreCase = true) }) {
                    "Windows already exposes a Bluetooth-related serial port."
                } else {
                    "Windows has not exposed an outgoing Bluetooth COM port yet, so the printer may not be paired or may not support Classic SPP."
                }
            }
            JvmOperatingSystem.LINUX -> {
                val paired = runProbe(listOf("bluetoothctl", "paired-devices"), timeoutMs = 2000)
                when {
                    !paired.available -> ""
                    paired.output.contains("Device", ignoreCase = true) -> "BlueZ reports paired Bluetooth devices."
                    else -> "BlueZ does not report paired devices yet."
                }
            }
            JvmOperatingSystem.MACOS -> {
                val profiler = runProbe(listOf("system_profiler", "SPBluetoothDataType"), timeoutMs = 2500)
                when {
                    !profiler.available -> ""
                    profiler.output.contains("Paired: Yes", ignoreCase = true) -> "macOS reports at least one paired Bluetooth device."
                    profiler.output.contains("Paired: No", ignoreCase = true) -> "macOS reports Bluetooth devices that are not paired."
                    else -> "macOS pairing status is not explicit in the system profile output."
                }
            }
            JvmOperatingSystem.OTHER -> ""
        }
    }

    private fun runProbe(command: List<String>, timeoutMs: Long): ProbeResult {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                ProbeResult(available = true, output = "Command timed out")
            } else {
                ProbeResult(
                    available = true,
                    output = process.inputStream.bufferedReader().readText()
                )
            }
        } catch (_: Exception) {
            ProbeResult(available = false, output = "")
        }
    }
}

internal fun JvmPrintQueueInfo.scoreBluetoothClassic(os: JvmOperatingSystem): Int {
    val normalizedName = name.lowercase()
    return buildList {
        if (looksLikeBluetooth) add(45)
        if (looksLikePrinter) add(20)
        when (os) {
            JvmOperatingSystem.WINDOWS -> {
                if (normalizedName.contains("bluetooth")) add(25)
                if (normalizedName.contains("bth")) add(15)
            }
            JvmOperatingSystem.LINUX -> {
                if (normalizedName.contains("bluetooth")) add(20)
                if (normalizedName.contains("cups")) add(10)
            }
            JvmOperatingSystem.MACOS -> {
                if (normalizedName.contains("bluetooth")) add(15)
                if (normalizedName.contains("airprint")) add(10)
            }
            JvmOperatingSystem.OTHER -> {
                if (normalizedName.contains("bluetooth")) add(10)
            }
        }
    }.sum().coerceIn(0, 100)
}

private data class ProbeResult(
    val available: Boolean,
    val output: String
)
