package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterBleFailureReason
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterSerialFailureReason
import ngga.ring.printer.model.PrinterTransportCapability
import ngga.ring.printer.model.PrinterUsbDiagnostic
import ngga.ring.printer.model.PrinterUsbFailureReason

actual class PrinterPlatformDiagnostics actual constructor() {
    actual fun getReport(): PrinterPlatformReport {
        return PrinterPlatformReport(
            platformName = "JS Browser",
            osName = "Browser",
            capabilities = listOf(
                capability(PrinterConnectionType.BLUETOOTH_LE, supported = true, native = true, discovery = false),
                capability(PrinterConnectionType.USB, supported = true, native = true, discovery = false),
                capability(PrinterConnectionType.NETWORK, supported = false, native = false, discovery = false),
                capability(PrinterConnectionType.VIRTUAL, supported = true, native = true, discovery = true)
            ),
            notes = listOf("Web Bluetooth and WebUSB require HTTPS and a user gesture.")
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.USB -> "WebUSB requires HTTPS, a user gesture, and browser support."
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> "Web Bluetooth requires HTTPS, a user gesture, and compatible services."
            else -> "No browser-specific troubleshooting hint available."
        }
    }

    actual fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return PrinterUsbDiagnostic(
            deviceFound = false,
            canOpen = false,
            canClaimInterface = false,
            failureReason = PrinterUsbFailureReason.UNSUPPORTED_PLATFORM,
            message = "Browser USB diagnostics are handled by WebUSB permission prompts, not JVM libusb.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.USB)
        )
    }

    actual fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic {
        return PrinterBleDiagnostic(
            adapterAvailable = true,
            deviceFound = false,
            serviceFound = false,
            writableCharacteristicFound = false,
            failureReason = PrinterBleFailureReason.UNKNOWN,
            message = "Browser BLE diagnostics are handled by Web Bluetooth permission prompts.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH_LE)
        )
    }

    actual fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        return PrinterSerialDiagnostic(
            portFound = false,
            canOpen = false,
            canWrite = false,
            failureReason = PrinterSerialFailureReason.UNSUPPORTED_PLATFORM,
            message = "Browser targets cannot inspect OS Bluetooth Classic SPP serial ports.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH)
        )
    }

    private fun capability(
        type: String,
        supported: Boolean,
        native: Boolean,
        discovery: Boolean
    ): PrinterTransportCapability {
        return PrinterTransportCapability(type, supported, native, discovery)
    }
}
