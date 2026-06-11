package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger

/**
 * Placeholder for JVM transports that need OS-native backends.
 *
 * Keep this connector available as the future insertion point for raw USB
 * (libusb/usb4java) and native BLE (WinRT/CoreBluetooth/BlueZ) without changing
 * the public factory contract.
 */
class JvmUnsupportedNativeConnector(
    private val reason: String
) : BasePrinterConnector() {
    override suspend fun connect(config: PrinterConfig): Boolean {
        PrinterLogger.warn("JvmUnsupportedNativeConnector", reason)
        return false
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = false

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() = Unit

    override fun isConnected(): Boolean = false
}
