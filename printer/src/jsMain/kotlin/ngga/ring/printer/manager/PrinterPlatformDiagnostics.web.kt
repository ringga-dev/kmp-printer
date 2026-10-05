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
                capability(PrinterConnectionType.SERIAL, supported = true, native = true, discovery = false),
                capability(PrinterConnectionType.NETWORK, supported = false, native = false, discovery = false),
                capability(PrinterConnectionType.VIRTUAL, supported = true, native = true, discovery = true)
            ),
            notes = listOf(
                "Web Bluetooth, WebUSB and Web Serial require HTTPS and a user gesture.",
                "Browser transports have no discovery API; the user always picks the device."
            )
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.USB -> "WebUSB requires HTTPS, a user gesture, and browser support."
            PrinterConnectionType.SERIAL -> "Web Serial requires HTTPS, a user gesture, and a port exposed by the printer."
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> "Web Bluetooth requires HTTPS, a user gesture, and compatible services."
            PrinterConnectionType.NETWORK -> "Browsers cannot open raw TCP sockets; use USB, serial or Bluetooth instead."
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
        // Serial is supported through Web Serial, but the browser gives no way
        // to enumerate ports, so the reported state always reflects "not yet
        // chosen" rather than "not present".
        return PrinterSerialDiagnostic(
            portFound = false,
            canOpen = false,
            canWrite = false,
            failureReason = PrinterSerialFailureReason.PORT_NOT_FOUND,
            message = "Web Serial requires the user to pick a port; browsers expose no port list.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.SERIAL)
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
