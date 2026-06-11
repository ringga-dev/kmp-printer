package ngga.ring.printer.model

/**
 * Configuration for connecting to a thermal printer.
 * Standalone DTO to remove dependency on :data module.
 */
data class PrinterConfig(
    val name: String,
    val connectionType: String,
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
) {
    constructor(
        name: String,
        connection: PrinterConnection,
        address: String? = null,
        port: Int = 9100,
        characterPerLine: Int = 31,
        paperWidth: Int = 58,
        paperWidthDots: Int = 0,
        leftMargin: Int = 0,
        autoCenter: Boolean = false,
        charsetName: String = "UTF-8",
        escPosCodePage: Byte = 0x00,
        connectionTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 2000,
        baudRate: Int = 9600,
        connectAttempts: Int = 2,
        sendAttempts: Int = 2,
        retryDelayMs: Long = 150,
        reconnectOnSendFailure: Boolean = true,
        sendChunkSize: Int = 512,
        sendChunkDelayMs: Long = 20,
        bleServiceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
        bleWriteCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
        bleAutoDiscover: Boolean = true,
        bleHandshakeEnabled: Boolean = true,
        bleChunkSize: Int = 20,
        bleCommandDelayMs: Long = 120,
        bleBridgeCommand: String? = null,
        bluetoothClassicAutoBind: Boolean = true,
        bluetoothClassicRfcommDevice: String = "/dev/rfcomm0",
    ) : this(
        name = name,
        connectionType = connection.value,
        address = address,
        port = port,
        characterPerLine = characterPerLine,
        paperWidth = paperWidth,
        paperWidthDots = paperWidthDots,
        leftMargin = leftMargin,
        autoCenter = autoCenter,
        charsetName = charsetName,
        escPosCodePage = escPosCodePage,
        connectionTimeoutMs = connectionTimeoutMs,
        readTimeoutMs = readTimeoutMs,
        baudRate = baudRate,
        connectAttempts = connectAttempts,
        sendAttempts = sendAttempts,
        retryDelayMs = retryDelayMs,
        reconnectOnSendFailure = reconnectOnSendFailure,
        sendChunkSize = sendChunkSize,
        sendChunkDelayMs = sendChunkDelayMs,
        bleServiceUuid = bleServiceUuid,
        bleWriteCharacteristicUuid = bleWriteCharacteristicUuid,
        bleAutoDiscover = bleAutoDiscover,
        bleHandshakeEnabled = bleHandshakeEnabled,
        bleChunkSize = bleChunkSize,
        bleCommandDelayMs = bleCommandDelayMs,
        bleBridgeCommand = bleBridgeCommand,
        bluetoothClassicAutoBind = bluetoothClassicAutoBind,
        bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice
    )

    constructor(
        name: String,
        connection: PrinterConnection,
        profile: PrinterProfile,
        address: String? = null,
        port: Int = 9100,
        charsetName: String = "UTF-8",
        escPosCodePage: Byte = 0x00,
        connectionTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 2000,
        baudRate: Int = 9600,
        connectAttempts: Int = 2,
        sendAttempts: Int = 2,
        retryDelayMs: Long = 150,
        reconnectOnSendFailure: Boolean = true,
        sendChunkSize: Int = 512,
        sendChunkDelayMs: Long = 20,
        bleServiceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
        bleWriteCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
        bleAutoDiscover: Boolean = true,
        bleHandshakeEnabled: Boolean = true,
        bleChunkSize: Int = 20,
        bleCommandDelayMs: Long = 120,
        bleBridgeCommand: String? = null,
        bluetoothClassicAutoBind: Boolean = true,
        bluetoothClassicRfcommDevice: String = "/dev/rfcomm0",
    ) : this(
        name = name,
        connection = connection,
        address = address,
        port = port,
        characterPerLine = profile.characterPerLine,
        paperWidth = profile.paperWidth,
        paperWidthDots = profile.paperWidthDots,
        leftMargin = profile.leftMargin,
        autoCenter = profile.autoCenter,
        charsetName = charsetName,
        escPosCodePage = escPosCodePage,
        connectionTimeoutMs = connectionTimeoutMs,
        readTimeoutMs = readTimeoutMs,
        baudRate = baudRate,
        connectAttempts = connectAttempts,
        sendAttempts = sendAttempts,
        retryDelayMs = retryDelayMs,
        reconnectOnSendFailure = reconnectOnSendFailure,
        sendChunkSize = sendChunkSize,
        sendChunkDelayMs = sendChunkDelayMs,
        bleServiceUuid = bleServiceUuid,
        bleWriteCharacteristicUuid = bleWriteCharacteristicUuid,
        bleAutoDiscover = bleAutoDiscover,
        bleHandshakeEnabled = bleHandshakeEnabled,
        bleChunkSize = bleChunkSize,
        bleCommandDelayMs = bleCommandDelayMs,
        bleBridgeCommand = bleBridgeCommand,
        bluetoothClassicAutoBind = bluetoothClassicAutoBind,
        bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice
    )

    val connection: PrinterConnection
        get() = PrinterConnection.from(connectionType)
}

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
