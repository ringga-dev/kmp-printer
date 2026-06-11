package ngga.ring.printer.manager

import com.fazecast.jSerialComm.SerialPort
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * JVM Implementation for Serial/USB/Bluetooth Serial printers using jSerialComm.
 */
class JvmSerialConnector : BasePrinterConnector() {
    private var serialPort: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            configureFlowControl(config)
            val portDescriptor = config.address ?: return@withContext false
            val port = SerialPort.getCommPort(portDescriptor)
            
            // Optimized settings for thermal printers
            port.baudRate = config.baudRate
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = SerialPort.NO_PARITY
            
            if (port.openPort()) {
                port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                    config.readTimeoutMs, 
                    config.connectionTimeoutMs
                )
                serialPort = port
                inputStream = port.inputStream
                outputStream = port.outputStream
                true
            } else {
                false
            }
        } catch (e: Exception) {
            PrinterLogger.warn("JvmSerialConnector", "Serial connection failed", e)
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val out = outputStream ?: return@withContext false
            out.write(data)
            out.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val port = serialPort ?: return@withContext null
            val input = inputStream ?: return@withContext null
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                timeout.toInt().coerceAtLeast(1),
                timeout.toInt().coerceAtLeast(1)
            )
            val buffer = ByteArray(count)
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            serialPort?.closePort()
            inputStream = null
            outputStream = null
            serialPort = null
        } catch (e: Exception) {}
    }

    override fun isConnected(): Boolean = serialPort?.isOpen ?: false
}

/**
 * JVM Bluetooth Classic connector.
 *
 * Bluetooth Classic is still exposed through the OS as a serial device or
 * printer queue on desktop JVMs. This connector makes that behavior explicit
 * and OS-aware so Windows, Linux, and macOS can rank different targets first.
 */
class JvmBluetoothClassicConnector(
    private val portService: JvmPrinterPortService = JvmPrinterPortService(),
    private val queueService: JvmPrintQueueService = JvmPrintQueueService()
) : BasePrinterConnector() {
    private val serialConnector = JvmSerialConnector()
    private val printQueueConnector = JvmPrintServiceConnector(queueService)
    private val backend: JvmBluetoothClassicBackend = when (portService.currentOs()) {
        JvmOperatingSystem.WINDOWS -> WindowsBluetoothClassicBackend()
        JvmOperatingSystem.LINUX -> LinuxBluetoothClassicBackend()
        JvmOperatingSystem.MACOS -> MacosBluetoothClassicBackend()
        JvmOperatingSystem.OTHER -> GenericBluetoothClassicBackend()
    }
    private var activeConnector: PrinterConnector? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        activeConnector = null

        val serialCandidates = backend.serialCandidates(config, portService.listSerialPorts())
        for (candidate in serialCandidates) {
            PrinterLogger.debug(TAG, "Bluetooth Classic ${backend.os} trying serial target ${candidate.address}")
            if (serialConnector.connect(config.copy(address = candidate.address))) {
                activeConnector = serialConnector
                PrinterLogger.info(TAG, "Bluetooth Classic ${backend.os} connected through serial target ${candidate.address}")
                return@withContext true
            }
            serialConnector.disconnect()
        }

        val queueCandidates = backend.queueCandidates(config, queueService.listQueues())
        for (candidate in queueCandidates) {
            PrinterLogger.debug(TAG, "Bluetooth Classic ${backend.os} trying print queue ${candidate.name}")
            if (printQueueConnector.connect(config.copy(address = candidate.name))) {
                activeConnector = printQueueConnector
                PrinterLogger.info(TAG, "Bluetooth Classic ${backend.os} connected through print queue ${candidate.name}")
                return@withContext true
            }
            printQueueConnector.disconnect()
        }

        PrinterLogger.warn(TAG, "Bluetooth Classic ${backend.os} connection failed. ${backend.failureHint(config, portService)}")
        false
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        return activeConnector?.sendData(data) ?: false
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? {
        return activeConnector?.readData(count, timeout)
    }

    override suspend fun disconnect() {
        activeConnector?.disconnect()
        serialConnector.disconnect()
        printQueueConnector.disconnect()
        activeConnector = null
    }

    override fun isConnected(): Boolean = activeConnector?.isConnected() == true
}

private interface JvmBluetoothClassicBackend {
    val os: JvmOperatingSystem

    fun serialCandidates(config: PrinterConfig, ports: List<JvmSerialPortInfo>): List<JvmSerialPortInfo>

    fun queueCandidates(config: PrinterConfig, queues: List<JvmPrintQueueInfo>): List<JvmPrintQueueInfo>

    fun failureHint(config: PrinterConfig, portService: JvmPrinterPortService): String
}

private abstract class BaseBluetoothClassicBackend(
    override val os: JvmOperatingSystem
) : JvmBluetoothClassicBackend {
    override fun serialCandidates(config: PrinterConfig, ports: List<JvmSerialPortInfo>): List<JvmSerialPortInfo> {
        val normalizedAddress = config.address?.trim().orEmpty()
        val exact = ports.firstOrNull { port ->
            port.address.equals(normalizedAddress, ignoreCase = true) ||
                port.name.equals(normalizedAddress, ignoreCase = true)
        }

        val orderedPorts = ports
            .map { port -> port to port.scoreBluetoothClassic(os) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (port, _) -> port }

        val prioritized = buildList {
            if (exact != null) add(exact)
            orderedPorts.forEach { port ->
                if (exact == null || port.address != exact.address) {
                    add(port)
                }
            }
            explicitSerialInfo(normalizedAddress)?.let { explicit ->
                if (exact == null && none { it.address.equals(explicit.address, ignoreCase = true) }) {
                    add(explicit)
                }
            }
        }

        return prioritized.distinctBy { it.address.lowercase() }
    }

    override fun queueCandidates(config: PrinterConfig, queues: List<JvmPrintQueueInfo>): List<JvmPrintQueueInfo> {
        val normalizedAddress = config.address?.trim().orEmpty()
        val exact = queues.firstOrNull { queue ->
            queue.name.equals(normalizedAddress, ignoreCase = true)
        }

        val orderedQueues = queues
            .map { queue -> queue to queue.scoreBluetoothClassic(os) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (queue, _) -> queue }

        val prioritized = buildList {
            if (exact != null) add(exact)
            orderedQueues.forEach { queue ->
                if (exact == null || queue.name != exact.name) {
                    add(queue)
                }
            }
        }

        return prioritized.distinctBy { it.name.lowercase() }
    }

    override fun failureHint(config: PrinterConfig, portService: JvmPrinterPortService): String {
        val address = config.address?.trim().orEmpty()
        val addressHint = when {
            address.isBlank() -> "No address was provided; discovery should be used to select the OS serial port or printer queue."
            isBluetoothMacAddress(address) -> "The configured address looks like a Bluetooth MAC address; desktop JVM printing needs the OS-exposed serial port or printer queue, not the MAC itself."
            else -> "Configured address was `$address`."
        }

        return "$addressHint ${portService.connectionHint(PrinterConnectionType.BLUETOOTH)}"
    }

    protected fun explicitSerialInfo(address: String): JvmSerialPortInfo? {
        if (address.isBlank() || !isExplicitSerialAddress(address)) return null
        return JvmSerialPortInfo(
            name = address,
            address = address,
            descriptiveName = address,
            looksLikePrinter = false,
            looksLikeBluetooth = true
        )
    }

    protected fun isBluetoothMacAddress(address: String): Boolean {
        return Regex("^[0-9A-Fa-f]{2}([:-][0-9A-Fa-f]{2}){5}$").matches(address)
    }

    protected open fun isExplicitSerialAddress(address: String): Boolean {
        val normalized = address.lowercase()
        return when (os) {
            JvmOperatingSystem.WINDOWS -> Regex("^com\\d+$", RegexOption.IGNORE_CASE).matches(address)
            JvmOperatingSystem.LINUX -> normalized.startsWith("/dev/tty") || normalized.startsWith("/dev/rfcomm")
            JvmOperatingSystem.MACOS -> normalized.startsWith("/dev/cu.") || normalized.startsWith("/dev/tty.")
            JvmOperatingSystem.OTHER -> normalized.startsWith("/dev/") || normalized.startsWith("com")
        }
    }
}

private class WindowsBluetoothClassicBackend : BaseBluetoothClassicBackend(JvmOperatingSystem.WINDOWS) {
    override fun serialCandidates(config: PrinterConfig, ports: List<JvmSerialPortInfo>): List<JvmSerialPortInfo> {
        return super.serialCandidates(config, ports).filter { port ->
            val address = port.address.lowercase()
            val name = port.name.lowercase()
            address.startsWith("com") || name.contains("bluetooth") || name.contains("bth")
        }
    }

    override fun failureHint(config: PrinterConfig, portService: JvmPrinterPortService): String {
        val base = super.failureHint(config, portService)
        return "$base Windows Bluetooth Classic requires the outgoing COM port created by pairing; incoming COM ports usually cannot print."
    }
}

private class LinuxBluetoothClassicBackend : BaseBluetoothClassicBackend(JvmOperatingSystem.LINUX) {
    override fun serialCandidates(config: PrinterConfig, ports: List<JvmSerialPortInfo>): List<JvmSerialPortInfo> {
        val candidates = super.serialCandidates(config, ports).toMutableList()
        val address = config.address?.trim().orEmpty()
        val boundDevice = prepareRfcommCandidate(config, address)
        if (boundDevice != null && candidates.none { it.address.equals(boundDevice, ignoreCase = true) }) {
            candidates.add(
                JvmSerialPortInfo(
                    name = boundDevice,
                    address = boundDevice,
                    descriptiveName = "Linux rfcomm Bluetooth Classic binding",
                    looksLikePrinter = false,
                    looksLikeBluetooth = true
                )
            )
        }
        return candidates.distinctBy { it.address.lowercase() }
    }

    override fun failureHint(config: PrinterConfig, portService: JvmPrinterPortService): String {
        val address = config.address?.trim().orEmpty()
        val base = if (isBluetoothMacAddress(address)) {
            "The configured address looks like a Bluetooth MAC address; Linux can bind it to ${config.bluetoothClassicRfcommDevice} when rfcomm permissions allow it."
        } else {
            super.failureHint(config, portService)
        }
        return "$base Linux Bluetooth Classic requires BlueZ pairing and a readable/writable /dev/rfcomm* or tty device."
    }

    private fun prepareRfcommCandidate(config: PrinterConfig, address: String): String? {
        if (!config.bluetoothClassicAutoBind) return null
        if (!isBluetoothMacAddress(address)) return null

        val rfcommDevice = config.bluetoothClassicRfcommDevice.ifBlank { "/dev/rfcomm0" }
        val existing = runCommand(listOf("rfcomm", "show", rfcommDevice), timeoutMs = 1500)
        if (existing.available && existing.output.contains(address, ignoreCase = true)) {
            return rfcommDevice
        }

        val bind = runCommand(listOf("rfcomm", "bind", rfcommDevice, address), timeoutMs = 2500)
        if (bind.available && bind.exitCode == 0) {
            PrinterLogger.info(TAG, "Bluetooth Classic bound $address to $rfcommDevice")
            return rfcommDevice
        }

        val detail = if (bind.available) bind.output.ifBlank { "exitCode=${bind.exitCode}" } else "rfcomm command is not available"
        PrinterLogger.warn(TAG, "Bluetooth Classic rfcomm bind skipped/failed for $address on $rfcommDevice: $detail")
        return rfcommDevice
    }
}

private class MacosBluetoothClassicBackend : BaseBluetoothClassicBackend(JvmOperatingSystem.MACOS) {
    override fun serialCandidates(config: PrinterConfig, ports: List<JvmSerialPortInfo>): List<JvmSerialPortInfo> {
        return super.serialCandidates(config, ports).filter { port ->
            val address = port.address.lowercase()
            address.startsWith("/dev/cu.") || address.startsWith("/dev/tty.")
        }
    }

    override fun failureHint(config: PrinterConfig, portService: JvmPrinterPortService): String {
        val base = super.failureHint(config, portService)
        return "$base macOS Bluetooth Classic requires an outgoing /dev/cu.* serial device or an installed printer queue."
    }
}

private class GenericBluetoothClassicBackend : BaseBluetoothClassicBackend(JvmOperatingSystem.OTHER)

private fun runCommand(command: List<String>, timeoutMs: Long): CommandProbe {
    return try {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!completed) {
            process.destroyForcibly()
            CommandProbe(available = true, exitCode = -1, output = "Command timed out")
        } else {
            CommandProbe(
                available = true,
                exitCode = process.exitValue(),
                output = process.inputStream.bufferedReader().readText()
            )
        }
    } catch (_: Exception) {
        CommandProbe(available = false, exitCode = -1, output = "")
    }
}

private data class CommandProbe(
    val available: Boolean,
    val exitCode: Int,
    val output: String
)

private const val TAG = "JvmBluetoothClassicConnector"
