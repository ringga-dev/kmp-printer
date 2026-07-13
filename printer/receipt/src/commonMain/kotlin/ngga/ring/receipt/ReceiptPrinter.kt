package ngga.ring.receipt

import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrintStatus
import kotlinx.coroutines.flow.first

/**
 * High-level receipt printer that combines connection (via [KmpPrinter])
 * with receipt-specific formatting (ESC/POS commands).
 *
 * Usage:
 * ```kotlin
 * val receipt = ReceiptPrinter()
 *
 * receipt.connect(
 *     name = "Kasir Epson",
 *     address = "192.168.1.100",
 *     port = 9100
 * )
 *
 * receipt.print {
 *     storeName("TOKO ABC")
 *     address("Jl. Merdeka No. 123")
 *     separator()
 *     items.forEach { item ->
 *         lineItem(item.name, item.qty, item.price)
 *     }
 *     separator()
 *     total("Rp 55.000")
 *     qrCode("order-123")
 *     lineFeeds(3)
 * }
 * ```
 */
public class ReceiptPrinter(
    private val printer: KmpPrinter = KmpPrinter()
) {
    private var lastConfig: PrinterConfig? = null

    /** Whether currently connected to a printer. */
    public val isConnected: Boolean
        get() = lastConfig != null

    /**
     * Connect to a receipt printer.
     *
     * @param name Printer name.
     * @param connectionType One of: "NETWORK", "BLUETOOTH", "USB", "SERIAL".
     * @param address IP address or device path.
     * @param port Port number (default: 9100 for TCP/IP).
     * @param timeoutMs Connection timeout.
     */
    public suspend fun connect(
        name: String,
        connectionType: String = "NETWORK",
        address: String? = null,
        port: Int = 9100,
        timeoutMs: Int = 5000
    ): Boolean = connect(
        PrinterConfig(name, connectionType, address, port, connectionTimeoutMs = timeoutMs)
    )

    /**
     * Connect using a [PrinterConfig].
     */
    public suspend fun connect(config: PrinterConfig): Boolean {
        val status = printer.testConnection(config)
        return if (status is PrintStatus.Success) {
            lastConfig = config
            true
        } else {
            lastConfig = null
            false
        }
    }

    /**
     * Print a receipt built with the [ReceiptBuilder] DSL.
     *
     * ```kotlin
     * receipt.print {
     *     storeName("TOKO ABC")
     *     lineItem("Kopi", 1, 15000)
     *     total("Rp 15.000")
     * }
     * ```
     */
    public suspend fun print(builder: ReceiptBuilder.() -> Unit): PrintStatus {
        val config = lastConfig
            ?: return PrintStatus.Error("Not connected. Call connect() first.")
        val receiptBytes = ReceiptBuilder().apply(builder).build()
        return try {
            val result = printer.printRaw(config, receiptBytes)
                .first { it !is PrintStatus.Processing }
            result
        } catch (e: Exception) {
            PrintStatus.Error("Print receipt failed: ${e.message}")
        }
    }

    /**
     * Print raw ESC/POS bytes (for custom receipt formats).
     */
    public suspend fun printRaw(bytes: ByteArray): PrintStatus {
        val config = lastConfig
            ?: return PrintStatus.Error("Not connected. Call connect() first.")
        return try {
            val result = printer.printRaw(config, bytes)
                .first { it !is PrintStatus.Processing }
            result
        } catch (e: Exception) {
            PrintStatus.Error("Print failed: ${e.message}")
        }
    }

    /**
     * Feed paper and cut.
     */
    public suspend fun feedAndCut(lines: Int = 3): PrintStatus {
        val bytes = ReceiptBuilder().apply {
            lineFeeds(lines)
            cut()
        }.build()
        return printRaw(bytes)
    }

    /**
     * Disconnect from the printer.
     */
    public suspend fun disconnect() {
        try {
            printer.disconnect()
        } catch (_: Exception) { /* ignore */ }
        lastConfig = null
    }
}
