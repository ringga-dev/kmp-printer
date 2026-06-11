package ngga.ring.printer.model

data class PrinterTransportCapability(
    val connectionType: String,
    val isSupported: Boolean,
    val isNative: Boolean,
    val supportsDiscovery: Boolean,
    val notes: List<String> = emptyList()
)

data class PrinterPlatformReport(
    val platformName: String,
    val osName: String,
    val capabilities: List<PrinterTransportCapability>,
    val notes: List<String> = emptyList()
) {
    fun capabilityFor(type: String): PrinterTransportCapability? {
        val normalizedType = PrinterConnectionType.normalize(type)
        return capabilities.firstOrNull { it.connectionType == normalizedType }
    }
}
