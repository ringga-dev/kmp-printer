package ngga.ring.printer.model

enum class PrinterErrorCode {
    UNKNOWN,
    INVALID_CONFIG,
    PERMISSION_DENIED,
    DEVICE_NOT_FOUND,
    DEVICE_BUSY,
    DRIVER_MISSING,
    CONNECTION_FAILED,
    SEND_FAILED,
    READ_FAILED,
    STATUS_UNSUPPORTED,
    TIMEOUT,
    UNSUPPORTED_TRANSPORT
}

sealed class PrintStatus {
    object Idle : PrintStatus()
    object Connecting : PrintStatus()
    object Processing : PrintStatus()
    object Sending : PrintStatus()
    object Success : PrintStatus()
    data class Error(
        val message: String,
        val code: PrinterErrorCode = PrinterErrorCode.UNKNOWN,
        val cause: String? = null
    ) : PrintStatus()
}
