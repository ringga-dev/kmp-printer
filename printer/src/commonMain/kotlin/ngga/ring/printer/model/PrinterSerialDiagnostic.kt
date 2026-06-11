package ngga.ring.printer.model

enum class PrinterSerialFailureReason {
    NONE,
    UNSUPPORTED_PLATFORM,
    INVALID_ADDRESS,
    PORT_NOT_FOUND,
    PORT_BUSY,
    PERMISSION_DENIED,
    OPEN_FAILED,
    WRITE_FAILED,
    UNKNOWN
}

data class PrinterSerialPortDiagnostic(
    val name: String,
    val address: String,
    val isBluetoothLike: Boolean,
    val isPrinterLike: Boolean,
    val confidence: Int,
    val notes: List<String> = emptyList()
)

data class PrinterSerialDiagnostic(
    val portFound: Boolean,
    val canOpen: Boolean,
    val canWrite: Boolean,
    val failureReason: PrinterSerialFailureReason = PrinterSerialFailureReason.NONE,
    val message: String = "",
    val suggestedFix: String = "",
    val ports: List<PrinterSerialPortDiagnostic> = emptyList()
) {
    val isReady: Boolean
        get() = portFound && canOpen && canWrite && failureReason == PrinterSerialFailureReason.NONE
}
