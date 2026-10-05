package ngga.ring.printer.manager

import kotlinx.coroutines.suspendCancellableCoroutine
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger
import kotlin.coroutines.resume

/**
 * Serial port connector for the browser, built on the Web Serial API.
 *
 * Many USB thermal printers expose a CH340/FTDI bridge and enumerate as a serial
 * port rather than as a USB printer class, so this complements [WebUsbConnector]
 * instead of duplicating it.
 *
 * Requires a secure context and a user gesture: `requestPort()` rejects
 * otherwise. There is no discovery API, so the user always picks the port.
 */
class WebSerialConnector : BasePrinterConnector() {
    private var port: dynamic = null
    private var writer: dynamic = null

    override suspend fun connect(config: PrinterConfig): Boolean =
        suspendCancellableCoroutine { continuation ->
            try {
                configureFlowControl(config)
                val serial = kotlinx.browser.window.navigator.asDynamic().serial
                if (serial == null) {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                serial.requestPort()
                    .then { requested: dynamic ->
                        port = requested
                        // Keep the reader locked-free: printers here are
                        // write-only, so only the writer side is needed.
                        requested.open(config.baudRate)
                    }
                    .then { opened: dynamic ->
                        writer = opened.writable.getWriter()
                        continuation.resume(true)
                    }
                    .catch { error: Throwable ->
                        PrinterLogger.warn(TAG, "Serial connection failed", error)
                        cleanup()
                        continuation.resume(false)
                    }
            } catch (e: Throwable) {
                PrinterLogger.warn(TAG, "Serial connection failed", e)
                cleanup()
                continuation.resume(false)
            }
        }

    override suspend fun sendRawData(data: ByteArray): Boolean =
        suspendCancellableCoroutine { continuation ->
            val activeWriter = writer ?: run {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            try {
                activeWriter.write(data.toTypedArray().asDynamic())
                    .then { continuation.resume(true) }
                    .catch {
                        // The writer is closed once the stream errors out;
                        // drop it so later writes fail fast instead of throwing.
                        writer = null
                        continuation.resume(false)
                    }
            } catch (e: Throwable) {
                writer = null
                continuation.resume(false)
            }
        }

    /** Serial printers are write-only here, so there is nothing to read back. */
    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() {
        cleanup()
    }

    override fun isConnected(): Boolean = writer != null

    private fun cleanup() {
        val activeWriter = writer
        writer = null
        if (activeWriter != null) {
            runCatching { activeWriter.releaseLock() }
        }
        val activePort = port
        port = null
        if (activePort != null) {
            runCatching { activePort.close() }
        }
    }

    private companion object {
        const val TAG = "WebSerialConnector"
    }
}