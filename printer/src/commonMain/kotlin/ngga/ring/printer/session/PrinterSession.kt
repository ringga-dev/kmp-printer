package ngga.ring.printer.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

/**
 * Represents an isolated, stateful connection session to a specific printer.
 * Allows multiple printers (e.g. Cashier + Kitchen) to be used concurrently
 * without cross-talk or race conditions.
 */
interface PrinterSession {
    val config: PrinterConfig
    val connectionState: StateFlow<ConnectionState>
    val isConnected: Boolean

    /**
     * Explicitly opens the connection to the printer.
     */
    suspend fun connect(): Boolean

    /**
     * Sends raw bytes to the printer.
     */
    suspend fun sendData(data: ByteArray): Boolean

    /**
     * Prints ESC/POS commands using a DSL builder.
     */
    suspend fun print(block: ESCPosCommandBuilder.() -> Unit): Flow<PrintStatus>

    /**
     * Prints a TSPL label using a label builder DSL.
     */
    suspend fun printLabel(
        widthMm: Double = 40.0,
        heightMm: Double = 30.0,
        block: ngga.ring.printer.util.tspl.TsplCommandBuilder.() -> Unit
    ): Flow<PrintStatus>

    /**
     * Sends raw byte stream with reactive progress and error reporting.
     */
    fun printRaw(data: ByteArray): Flow<PrintStatus>

    /**
     * Pulses the cash drawer kick pin to open the cash drawer.
     * @param pin Drawer kick pin (0 for Pin 2, 1 for Pin 5).
     * @return true if command was sent successfully.
     */
    suspend fun openCashDrawer(pin: Int = 0): Boolean

    /**
     * Sends a beep sound command to the printer buzzer.
     * @param count Number of beeps (1..9).
     * @param duration Duration multiplier (1..9 * 100ms).
     * @return true if command was sent successfully.
     */
    suspend fun beep(count: Int = 1, duration: Int = 1): Boolean

    /**
     * Queries printer hardware status once.
     */
    suspend fun queryStatus(): PrinterStatus

    /**
     * Monitors real-time printer status (paper out, cover open, online).
     */
    fun monitorStatus(intervalMs: Long = 2000): Flow<PrinterStatus>

    /**
     * Disconnects this session and frees underlying socket/stream resources.
     */
    suspend fun disconnect()
}
