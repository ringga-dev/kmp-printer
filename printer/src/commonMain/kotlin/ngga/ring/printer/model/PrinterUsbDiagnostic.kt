package ngga.ring.printer.model

enum class PrinterUsbFailureReason {
    NONE,
    UNSUPPORTED_PLATFORM,
    INVALID_ADDRESS,
    LIBUSB_INIT_FAILED,
    DEVICE_NOT_FOUND,
    NO_BULK_OUT_ENDPOINT,
    ACCESS_DENIED,
    DRIVER_NOT_COMPATIBLE,
    INTERFACE_BUSY,
    CLAIM_FAILED,
    TRANSFER_FAILED,
    UNKNOWN
}

data class PrinterUsbDiagnostic(
    val deviceFound: Boolean,
    val canOpen: Boolean,
    val canClaimInterface: Boolean,
    val failureReason: PrinterUsbFailureReason = PrinterUsbFailureReason.NONE,
    val message: String = "",
    val suggestedFix: String = "",
    val udevRule: String? = null
) {
    val isReady: Boolean
        get() = deviceFound && canOpen && canClaimInterface && failureReason == PrinterUsbFailureReason.NONE
}
