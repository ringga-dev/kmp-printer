package ngga.ring.printer.model

enum class PrinterBleFailureReason {
    NONE,
    UNSUPPORTED_PLATFORM,
    INVALID_ADDRESS,
    ADAPTER_UNAVAILABLE,
    ADAPTER_POWERED_OFF,
    DEVICE_NOT_FOUND,
    SERVICE_NOT_FOUND,
    CHARACTERISTIC_NOT_FOUND,
    PERMISSION_DENIED,
    BACKEND_NOT_INSTALLED,
    NATIVE_BACKEND_NOT_IMPLEMENTED,
    UNKNOWN
}

data class PrinterBleDiagnostic(
    val adapterAvailable: Boolean,
    val deviceFound: Boolean,
    val serviceFound: Boolean,
    val writableCharacteristicFound: Boolean,
    val failureReason: PrinterBleFailureReason = PrinterBleFailureReason.NONE,
    val message: String = "",
    val suggestedFix: String = ""
) {
    val isReady: Boolean
        get() = adapterAvailable &&
            deviceFound &&
            serviceFound &&
            writableCharacteristicFound &&
            failureReason == PrinterBleFailureReason.NONE
}
