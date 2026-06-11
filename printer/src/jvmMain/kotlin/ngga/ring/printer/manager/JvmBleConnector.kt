package ngga.ring.printer.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterBleFailureReason
import ngga.ring.printer.model.PrinterConfig
import java.util.concurrent.TimeUnit

interface JvmBleBackend {
    val os: JvmOperatingSystem
    fun diagnose(config: PrinterConfig): PrinterBleDiagnostic
    fun open(config: PrinterConfig): JvmBleSession?
    fun troubleshootingHint(): String
}

interface JvmBleSession {
    fun write(data: ByteArray): Boolean
    fun close()
}

abstract class BaseJvmBleBackend(
    override val os: JvmOperatingSystem
) : JvmBleBackend {
    override fun open(config: PrinterConfig): JvmBleSession? = null

    override fun diagnose(config: PrinterConfig): PrinterBleDiagnostic {
        return PrinterBleDiagnostic(
            adapterAvailable = false,
            deviceFound = false,
            serviceFound = false,
            writableCharacteristicFound = false,
            failureReason = PrinterBleFailureReason.NATIVE_BACKEND_NOT_IMPLEMENTED,
            message = "Native JVM BLE backend for ${os.name} is not implemented yet.",
            suggestedFix = troubleshootingHint()
        )
    }

    protected fun diagnostic(
        adapterAvailable: Boolean,
        reason: PrinterBleFailureReason,
        message: String,
        fix: String = troubleshootingHint()
    ): PrinterBleDiagnostic {
        return PrinterBleDiagnostic(
            adapterAvailable = adapterAvailable,
            deviceFound = false,
            serviceFound = false,
            writableCharacteristicFound = false,
            failureReason = reason,
            message = message,
            suggestedFix = fix
        )
    }
}

class WindowsBleBackend : BaseJvmBleBackend(JvmOperatingSystem.WINDOWS) {
    override fun open(config: PrinterConfig): JvmBleSession? {
        return JvmBleBridgeProcess.open(
            config = config,
            defaultCommand = "kmp-printer-ble-windows.exe",
            backendName = "Windows WinRT BLE bridge"
        )
    }

    override fun diagnose(config: PrinterConfig): PrinterBleDiagnostic {
        val service = JvmCommand.run(
            listOf(
                "powershell",
                "-NoProfile",
                "-Command",
                "(Get-Service bthserv -ErrorAction SilentlyContinue).Status"
            )
        )

        if (!service.available) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.BACKEND_NOT_INSTALLED,
                message = "PowerShell Bluetooth service inspection is not available.",
                fix = "Enable PowerShell access or verify Bluetooth manually in Windows Settings. ${troubleshootingHint()}"
            )
        }

        val running = service.output.contains("Running", ignoreCase = true)
        if (!running) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.ADAPTER_POWERED_OFF,
                message = "Windows Bluetooth service is not running.",
                fix = "Start the Windows Bluetooth Support Service and enable Bluetooth, then retry."
            )
        }

        val bridge = JvmBleBridgeProcess.probe(config.bleBridgeCommand ?: "kmp-printer-ble-windows.exe")
        if (!bridge.available) {
            return diagnostic(
                adapterAvailable = true,
                reason = PrinterBleFailureReason.BACKEND_NOT_INSTALLED,
                message = "Windows Bluetooth service is running, but the BLE bridge executable was not found.",
                fix = "Provide `kmp-printer-ble-windows.exe` on PATH or set PrinterConfig.bleBridgeCommand to the helper path."
            )
        }

        return diagnostic(
            adapterAvailable = true,
            reason = PrinterBleFailureReason.NONE,
            message = "Windows BLE bridge is available. WinRT BLE transport can be used through the helper process.",
            fix = "Make sure the printer is paired or discoverable and the helper has Bluetooth permission."
        )
    }

    override fun troubleshootingHint(): String {
        return "Windows BLE uses an external WinRT helper process. Put kmp-printer-ble-windows.exe on PATH or set PrinterConfig.bleBridgeCommand."
    }
}

class LinuxBleBackend : BaseJvmBleBackend(JvmOperatingSystem.LINUX) {
    override fun open(config: PrinterConfig): JvmBleSession? {
        val address = config.address ?: return null
        val diagnostic = diagnose(config)
        if (!diagnostic.adapterAvailable) return null

        return try {
            val session = BluetoothCtlGattSession(
                address = address,
                serviceUuid = config.bleServiceUuid,
                characteristicUuid = config.bleWriteCharacteristicUuid,
                autoDiscover = config.bleAutoDiscover,
                handshakeEnabled = config.bleHandshakeEnabled,
                chunkSize = config.bleChunkSize.coerceIn(1, 512),
                commandDelayMs = config.bleCommandDelayMs.coerceAtLeast(20)
            )
            if (session.open()) session else null
        } catch (_: Exception) {
            null
        }
    }

    override fun diagnose(config: PrinterConfig): PrinterBleDiagnostic {
        if (config.address.isNullOrBlank()) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.INVALID_ADDRESS,
                message = "BLE printer address is empty.",
                fix = "Set PrinterConfig.address to the BLE MAC address, then retry."
            )
        }

        val bluetoothctl = JvmCommand.run(listOf("bluetoothctl", "show"))
        if (!bluetoothctl.available) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.BACKEND_NOT_INSTALLED,
                message = "bluetoothctl is not available.",
                fix = "Install BlueZ, start bluetoothd, and make sure bluetoothctl is available on PATH."
            )
        }

        val noController = bluetoothctl.output.contains("No default controller", ignoreCase = true)
        if (noController) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.ADAPTER_UNAVAILABLE,
                message = "BlueZ is available, but no default Bluetooth controller was found.",
                fix = "Attach/enable a Bluetooth adapter, unblock it with rfkill if needed, then retry."
            )
        }

        val powered = bluetoothctl.output.lineSequence().any {
            it.trim().equals("Powered: yes", ignoreCase = true)
        }
        if (!powered) {
            return diagnostic(
                adapterAvailable = true,
                reason = PrinterBleFailureReason.ADAPTER_POWERED_OFF,
                message = "Bluetooth adapter exists, but it is powered off.",
                fix = "Run `bluetoothctl power on` or enable Bluetooth in system settings, then retry."
            )
        }

        val info = JvmCommand.run(listOf("bluetoothctl", "info", config.address), timeoutMs = 2500)
        if (info.available && info.output.contains("Device ${config.address} not available", ignoreCase = true)) {
            return diagnostic(
                adapterAvailable = true,
                reason = PrinterBleFailureReason.DEVICE_NOT_FOUND,
                message = "BLE device ${config.address} is not known to BlueZ.",
                fix = "Pair or scan the printer with bluetoothctl first, then retry."
            )
        }

        return PrinterBleDiagnostic(
            adapterAvailable = true,
            deviceFound = info.available && info.output.contains("Device", ignoreCase = true),
            serviceFound = false,
            writableCharacteristicFound = false,
            failureReason = PrinterBleFailureReason.NONE,
            message = "BlueZ adapter is ready. Linux BLE transport will use bluetoothctl GATT write commands.",
            suggestedFix = "If printing fails, verify service ${config.bleServiceUuid} and characteristic ${config.bleWriteCharacteristicUuid} with bluetoothctl menu gatt."
        )
    }

    override fun troubleshootingHint(): String {
        return "Linux BLE uses BlueZ bluetoothctl GATT write support. Make sure bluetoothd is running and the printer is paired or discoverable."
    }
}

class MacosBleBackend : BaseJvmBleBackend(JvmOperatingSystem.MACOS) {
    override fun open(config: PrinterConfig): JvmBleSession? {
        return JvmBleBridgeProcess.open(
            config = config,
            defaultCommand = "kmp-printer-ble-macos",
            backendName = "macOS CoreBluetooth BLE bridge"
        )
    }

    override fun diagnose(config: PrinterConfig): PrinterBleDiagnostic {
        val profiler = JvmCommand.run(listOf("system_profiler", "SPBluetoothDataType"))
        if (!profiler.available) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.BACKEND_NOT_INSTALLED,
                message = "macOS Bluetooth system profiler is not available.",
                fix = troubleshootingHint()
            )
        }

        val off = profiler.output.contains("Bluetooth Power: Off", ignoreCase = true)
        val unavailable = profiler.output.contains("No information found", ignoreCase = true)
        if (unavailable) {
            return diagnostic(
                adapterAvailable = false,
                reason = PrinterBleFailureReason.ADAPTER_UNAVAILABLE,
                message = "macOS did not report a Bluetooth adapter.",
                fix = "Enable or attach a Bluetooth adapter, then retry."
            )
        }
        if (off) {
            return diagnostic(
                adapterAvailable = true,
                reason = PrinterBleFailureReason.ADAPTER_POWERED_OFF,
                message = "macOS Bluetooth adapter is powered off.",
                fix = "Enable Bluetooth in macOS settings, then retry."
            )
        }

        val bridge = JvmBleBridgeProcess.probe(config.bleBridgeCommand ?: "kmp-printer-ble-macos")
        if (!bridge.available) {
            return diagnostic(
                adapterAvailable = true,
                reason = PrinterBleFailureReason.BACKEND_NOT_INSTALLED,
                message = "macOS Bluetooth appears available, but the BLE bridge executable was not found.",
                fix = "Provide `kmp-printer-ble-macos` on PATH or set PrinterConfig.bleBridgeCommand to the helper path."
            )
        }

        return diagnostic(
            adapterAvailable = true,
            reason = PrinterBleFailureReason.NONE,
            message = "macOS BLE bridge is available. CoreBluetooth transport can be used through the helper process.",
            fix = "Make sure macOS grants Bluetooth permission to the helper."
        )
    }

    override fun troubleshootingHint(): String {
        return "macOS BLE uses an external CoreBluetooth helper process. Put kmp-printer-ble-macos on PATH or set PrinterConfig.bleBridgeCommand."
    }
}

class GenericBleBackend : BaseJvmBleBackend(JvmOperatingSystem.OTHER) {
    override fun troubleshootingHint(): String {
        return "Native BLE is not available for this JVM OS backend."
    }
}

class JvmBleService {
    private val backend: JvmBleBackend = when (JvmOperatingSystem.current()) {
        JvmOperatingSystem.WINDOWS -> WindowsBleBackend()
        JvmOperatingSystem.LINUX -> LinuxBleBackend()
        JvmOperatingSystem.MACOS -> MacosBleBackend()
        JvmOperatingSystem.OTHER -> GenericBleBackend()
    }

    fun currentOs(): JvmOperatingSystem = backend.os

    fun troubleshootingHint(): String = backend.troubleshootingHint()

    fun diagnose(config: PrinterConfig): PrinterBleDiagnostic = backend.diagnose(config)

    fun open(config: PrinterConfig): JvmBleSession? = backend.open(config)
}

class JvmBleConnector(
    private val bleService: JvmBleService = JvmBleService()
) : BasePrinterConnector() {
    private var session: JvmBleSession? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        session = bleService.open(config)
        session != null
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        session?.write(data) ?: false
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() {
        session?.close()
        session = null
    }

    override fun isConnected(): Boolean = session != null
}

private data class CommandResult(
    val available: Boolean,
    val output: String
)

private object JvmCommand {
    fun run(command: List<String>, timeoutMs: Long = 1500): CommandResult {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                return CommandResult(available = true, output = "Command timed out")
            }

            CommandResult(
                available = true,
                output = process.inputStream.bufferedReader().readText()
            )
        } catch (_: Exception) {
            CommandResult(available = false, output = "")
        }
    }
}

private class BluetoothCtlGattSession(
    private val address: String,
    private val serviceUuid: String,
    private val characteristicUuid: String,
    private val autoDiscover: Boolean,
    private val handshakeEnabled: Boolean,
    private val chunkSize: Int,
    private val commandDelayMs: Long
) : JvmBleSession {
    private var process: Process? = null
    private var writer: java.io.BufferedWriter? = null

    fun open(): Boolean {
        process = ProcessBuilder("bluetoothctl")
            .redirectErrorStream(true)
            .start()
        writer = process?.outputStream?.bufferedWriter()

        sendCommand("connect $address")
        sendCommand("menu gatt")
        if (autoDiscover) {
            sendCommand("list-attributes")
        }
        sendCommand("select-attribute $characteristicUuid")
        if (handshakeEnabled) {
            write(byteArrayOf(0x1B, 0x40))
        }
        return process?.isAlive == true
    }

    override fun write(data: ByteArray): Boolean {
        val activeProcess = process ?: return false
        if (!activeProcess.isAlive) return false

        data.toList()
            .chunked(chunkSize)
            .forEach { chunk ->
                val payload = chunk.joinToString(" ") { byte ->
                    "0x${(byte.toInt() and 0xFF).toString(16).padStart(2, '0')}"
                }
                sendCommand("write $payload")
            }
        return activeProcess.isAlive
    }

    override fun close() {
        try {
            sendCommand("disconnect $address")
            sendCommand("quit")
        } catch (_: Exception) {
        } finally {
            writer = null
            process?.destroy()
            process = null
        }
    }

    private fun sendCommand(command: String) {
        val currentWriter = writer ?: return
        currentWriter.write(command)
        currentWriter.newLine()
        currentWriter.flush()
        Thread.sleep(commandDelayMs)
    }
}

private object JvmBleBridgeProcess {
    fun probe(command: String): CommandResult {
        return JvmCommand.run(splitCommand(command) + "--version", timeoutMs = 1200)
    }

    fun open(config: PrinterConfig, defaultCommand: String, backendName: String): JvmBleSession? {
        val address = config.address ?: return null
        val command = splitCommand(config.bleBridgeCommand ?: defaultCommand)
        if (command.isEmpty()) return null

        return try {
            val args = command + listOf(
                "--connect",
                address,
                "--service",
                if (config.bleAutoDiscover) "auto" else config.bleServiceUuid,
                "--characteristic",
                if (config.bleAutoDiscover) "auto" else config.bleWriteCharacteristicUuid
            ) + if (config.bleHandshakeEnabled) listOf("--handshake") else emptyList()
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            val session = BridgeBleSession(
                process = process,
                backendName = backendName,
                chunkSize = config.bleChunkSize.coerceIn(1, 512)
            )
            if (session.awaitReady(config.connectionTimeoutMs.toLong())) session else null
        } catch (e: Exception) {
            println("PrinterJVM: $backendName failed to start: ${e.message}")
            null
        }
    }

    private fun splitCommand(command: String): List<String> {
        return command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
}

private class BridgeBleSession(
    private val process: Process,
    private val backendName: String,
    private val chunkSize: Int
) : JvmBleSession {
    private val writer = process.outputStream.bufferedWriter()

    fun awaitReady(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs.coerceAtLeast(250)
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive) return false
            return true
        }
        return process.isAlive
    }

    override fun write(data: ByteArray): Boolean {
        if (!process.isAlive) return false
        return try {
            data.toList().chunked(chunkSize).forEach { chunk ->
                writer.write(chunk.joinToString("") { byte ->
                    (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
                })
                writer.newLine()
                writer.flush()
            }
            true
        } catch (e: Exception) {
            println("PrinterJVM: $backendName write failed: ${e.message}")
            false
        }
    }

    override fun close() {
        try {
            writer.write("QUIT")
            writer.newLine()
            writer.flush()
        } catch (_: Exception) {
        } finally {
            process.destroy()
        }
    }
}
