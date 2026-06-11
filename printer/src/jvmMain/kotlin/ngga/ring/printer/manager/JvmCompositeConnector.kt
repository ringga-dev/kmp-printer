package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig

/**
 * Tries several JVM transports for the same logical connection type.
 *
 * This keeps the old serial fallback behavior while allowing OS printer queues
 * to handle USB/Bluetooth devices that do not expose a serial port.
 */
class JvmCompositeConnector(
    private val candidates: List<PrinterConnector>
) : BasePrinterConnector() {
    private var active: PrinterConnector? = null

    override suspend fun connect(config: PrinterConfig): Boolean {
        active?.disconnect()
        active = null

        for (candidate in candidates) {
            if (candidate.connect(config)) {
                active = candidate
                return true
            }
        }
        return false
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        return active?.sendData(data) ?: false
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? {
        return active?.readData(count, timeout)
    }

    override suspend fun disconnect() {
        active?.disconnect()
        active = null
    }

    override fun isConnected(): Boolean = active?.isConnected() == true
}
