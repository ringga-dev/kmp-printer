package ngga.ring.printer.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android Implementation for Network (LAN).
 */
class AndroidNetworkConnector : BasePrinterConnector() {
    private var socket: Socket? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            configureFlowControl(config)
            socket = Socket()
            socket?.tcpNoDelay = true
            socket?.keepAlive = true
            socket?.connect(
                InetSocketAddress(config.address ?: "127.0.0.1", config.port),
                config.connectionTimeoutMs
            )
            socket?.soTimeout = config.readTimeoutMs
            isConnected()
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Network connection failed", e)
            socket = null
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val current = socket ?: return@withContext false
            if (!isConnected()) return@withContext false
            current.getOutputStream().write(data)
            current.getOutputStream().flush()
            true
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Network send failed", e)
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val input = socket?.getInputStream() ?: return@withContext null
            
            // Wait for data with timeout
            val start = System.currentTimeMillis()
            while (input.available() <= 0) {
                if (System.currentTimeMillis() - start > timeout) return@withContext null
                kotlinx.coroutines.delay(10)
            }
            
            val buffer = ByteArray(count.coerceAtMost(input.available()))
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Network status read failed", e)
            null
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Network disconnect failed", e)
        }
    }

    override fun isConnected(): Boolean {
        val current = socket ?: return false
        return current.isConnected && !current.isClosed && !current.isOutputShutdown
    }

    private companion object {
        const val TAG = "AndroidNetworkConnector"
    }
}
