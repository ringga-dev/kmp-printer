package ngga.ring.printer.nativebridge

import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrintStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

// ──────────────────────────────────────────────
// Thread-safe registry of printer instances
// exposed as opaque Long handles to C callers
// ──────────────────────────────────────────────

private val lock = Any()
private val instances = mutableMapOf<Long, KmpPrinter>()  
private var nextHandle = 1L
private var lastError = ""

// ─── Lifecycle ────────────────────────────────

@CName("kmp_printer_create")
fun kmpPrinterCreate(): Long = synchronized(lock) {
    val handle = nextHandle++
    instances[handle] = KmpPrinter()
    lastError = ""
    handle
}

@CName("kmp_printer_destroy")
fun kmpPrinterDestroy(handle: Long) = synchronized(lock) {
    instances.remove(handle)
    if (instances.isEmpty()) nextHandle = 1L
    lastError = ""
}

// ─── Connection ───────────────────────────────

@CName("kmp_printer_connect")
fun kmpPrinterConnect(
    handle: Long,
    name: String,
    connectionType: String,     // "NETWORK", "BLUETOOTH", "USB", "SERIAL"
    address: String?,           // IP / MAC / VID:PID / null for auto
    port: Int,                  // 9100 default
    timeoutMs: Int              // 5000 default
): Int {
    val printer = synchronized(lock) { instances[handle] }
        ?: return errorResult("Invalid handle $handle")

    val config = PrinterConfig(
        name = name,
        connectionType = connectionType,
        address = address,
        port = port,
        connectionTimeoutMs = timeoutMs
    )

    return try {
        val status = runBlocking { printer.testConnection(config) }
        if (status is PrintStatus.Success) {
            clearError()
            1
        } else {
            val msg = (status as? PrintStatus.Error)?.message ?: "Connection failed"
            errorResult(msg)
        }
    } catch (e: Exception) {
        errorResult(e.message ?: "Connection error")
    }
}

@CName("kmp_printer_disconnect")
fun kmpPrinterDisconnect(handle: Long): Int {
    val printer = synchronized(lock) { instances[handle] }
        ?: return errorResult("Invalid handle $handle")
    return try {
        runBlocking { printer.disconnect() }
        clearError()
        1
    } catch (e: Exception) {
        errorResult(e.message ?: "Disconnect error")
    }
}

// ─── Printing ─────────────────────────────────

@CName("kmp_printer_print_text")
fun kmpPrinterPrintText(
    handle: Long,
    text: String
): Int {
    val printer = synchronized(lock) { instances[handle] }
        ?: return errorResult("Invalid handle $handle")

    // Build a minimal TCP/NETWORK config — caller must connect first
    // For production you'd persist the last-used config per handle
    val config = PrinterConfig(
        name = "c-printer",
        connectionType = "NETWORK",
        address = null,
        port = 9100
    )

    return try {
        val data = text.encodeToByteArray()
        val status = runBlocking { printer.printRaw(config, data).first { it !is PrintStatus.Processing } }
        when (status) {
            is PrintStatus.Success -> { clearError(); 1 }
            is PrintStatus.Error -> errorResult(status.message)
            else -> errorResult("Print failed with no status")
        }
    } catch (e: Exception) {
        errorResult(e.message ?: "Print error")
    }
}

@CName("kmp_printer_print_raw")
fun kmpPrinterPrintRaw(
    handle: Long,
    data: String     // C string (can contain null-bytes; in Kotlin all chars pass through)
): Int {
    val printer = synchronized(lock) { instances[handle] }
        ?: return errorResult("Invalid handle $handle")

    val config = PrinterConfig(
        name = "c-printer",
        connectionType = "NETWORK",
        address = null,
        port = 9100
    )

    return try {
        val bytes = data.encodeToByteArray()
        val status = runBlocking { printer.printRaw(config, bytes).first { it !is PrintStatus.Processing } }
        when (status) {
            is PrintStatus.Success -> { clearError(); 1 }
            is PrintStatus.Error -> errorResult(status.message)
            else -> errorResult("Print failed")
        }
    } catch (e: Exception) {
        errorResult(e.message ?: "Print error")
    }
}

@CName("kmp_printer_print_test_page")
fun kmpPrinterPrintTestPage(handle: Long): Int {
    val printer = synchronized(lock) { instances[handle] }
        ?: return errorResult("Invalid handle $handle")

    val config = PrinterConfig(
        name = "c-printer",
        connectionType = "NETWORK",
        address = null,
        port = 9100
    )

    return try {
        val status = runBlocking { printer.printTestPage(config).first { it !is PrintStatus.Processing } }
        when (status) {
            is PrintStatus.Success -> { clearError(); 1 }
            is PrintStatus.Error -> errorResult(status.message)
            else -> errorResult("Test page failed")
        }
    } catch (e: Exception) {
        errorResult(e.message ?: "Test page error")
    }
}

// ─── Error handling ───────────────────────────

@CName("kmp_printer_last_error")
fun kmpPrinterLastError(handle: Long): String = synchronized(lock) {
    lastError
}

// ─── Logger ──────────────────────────────────

@CName("kmp_printer_set_logger")
fun kmpPrinterSetLogger(handle: Long, enabled: Int) {
    val printer = synchronized(lock) { instances[handle] } ?: return
    if (enabled != 0) {
        printer.setLogger { event -> /* log to stderr handled by C side */ }
    } else {
        printer.setLogger(null)
    }
}

// ─── Helpers ─────────────────────────────────

private fun clearError() { lastError = "" }

private fun errorResult(msg: String): Int {
    synchronized(lock) { lastError = msg }
    return 0
}
