package ngga.ring.printer.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import ngga.ring.printer.model.PrinterConfig

/**
 * Base implementation of [PrinterConnector] that provides concurrency protection
 * and flow control (chunked sending).
 */
abstract class BasePrinterConnector : PrinterConnector {
    private val mutex = Mutex()
    private var chunkSize: Int = 512
    private var chunkDelayMs: Long = 20

    /**
     * Internal implementation for sending raw data. 
     * To be implemented by platform-specific connectors.
     */
    protected abstract suspend fun sendRawData(data: ByteArray): Boolean

    protected fun configureFlowControl(config: PrinterConfig) {
        chunkSize = config.sendChunkSize.coerceAtLeast(64)
        chunkDelayMs = config.sendChunkDelayMs.coerceAtLeast(0)
    }

    /**
     * Public sendData with Mutex protection to prevent interleaving bytes 
     * from concurrent print jobs.
     */
    override suspend fun sendData(data: ByteArray): Boolean = mutex.withLock {
        var success = true
        
        var offset = 0
        while (offset < data.size) {
            val end = (offset + chunkSize).coerceAtMost(data.size)
            val chunk = data.copyOfRange(offset, end)
            
            if (!sendRawData(chunk)) {
                success = false
                break
            }
            
            offset += chunkSize
            // Small pause for the printer's mechanical buffer to breathe
            if (offset < data.size) {
                delay(chunkDelayMs)
            }
        }
        
        return success
    }
}
