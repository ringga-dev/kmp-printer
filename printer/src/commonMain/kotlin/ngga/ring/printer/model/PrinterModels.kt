package ngga.ring.printer.model

/**
 * Configuration for connecting to a thermal printer.
 * Standalone DTO to remove dependency on :data module.
 */
data class PrinterConfig(
    val name: String,
    val connectionType: String, // "BLUETOOTH", "USB", "NETWORK"
    val address: String? = null, // MAC for BT, IP for Network, VID:PID for USB
    val port: Int = 9100,
    val characterPerLine: Int = 31,
    val paperWidth: Int = 58,
    val paperWidthDots: Int = 0, // Physical Hardware Dots (e.g. 384 or 576)
    val leftMargin: Int = 0,
    val autoCenter: Boolean = false,
    val charsetName: String = "UTF-8",
    val escPosCodePage: Byte = 0x00,
    val connectionTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 2000,
    val baudRate: Int = 9600,
    val connectAttempts: Int = 2,
    val sendAttempts: Int = 2,
    val retryDelayMs: Long = 150,
    val reconnectOnSendFailure: Boolean = true,
    val sendChunkSize: Int = 512,
    val sendChunkDelayMs: Long = 20,
    val bleServiceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
    val bleWriteCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
    val bleAutoDiscover: Boolean = true,
    val bleHandshakeEnabled: Boolean = true,
    val bleChunkSize: Int = 20,
    val bleCommandDelayMs: Long = 120,
    val bleBridgeCommand: String? = null,
    val bluetoothClassicAutoBind: Boolean = true,
    val bluetoothClassicRfcommDevice: String = "/dev/rfcomm0",
)

/**
 * Detailed real-time status of the printer.
 */
data class PrinterStatus(
    val isOnline: Boolean = true,
    val isCoverOpen: Boolean = false,
    val isPaperOut: Boolean = false,
    val isPaperNearEnd: Boolean = false,
    val isError: Boolean = false,
    val rawBytes: ByteArray? = null,
    val isStatusSupported: Boolean = true,
    val message: String = ""
)

/**
 * Real-time events emitted by the printer monitor.
 */
sealed class PrinterStatusEvent {
    object Online : PrinterStatusEvent()
    object Offline : PrinterStatusEvent()
    object CoverOpen : PrinterStatusEvent()
    object PaperOut : PrinterStatusEvent()
    object PaperNearEnd : PrinterStatusEvent()
    data class Error(val message: String) : PrinterStatusEvent()
}

/**
 * Result from a printer discovery process.
 */
data class DiscoveredPrinter(
    val name: String,
    val connectionType: String,
    val address: String,
    val port: Int = 9100
)
