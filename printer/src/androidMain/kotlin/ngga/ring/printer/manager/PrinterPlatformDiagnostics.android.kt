package ngga.ring.printer.manager

import android.os.Build
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterBleFailureReason
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterTransportCapability
import ngga.ring.printer.model.PrinterUsbDiagnostic
import ngga.ring.printer.model.PrinterUsbFailureReason

actual class PrinterPlatformDiagnostics actual constructor() {
    actual fun getReport(): PrinterPlatformReport {
        return PrinterPlatformReport(
            platformName = "Android",
            osName = "Android ${Build.VERSION.RELEASE}",
            capabilities = listOf(
                capability(PrinterConnectionType.NETWORK, native = true, discovery = true),
                capability(PrinterConnectionType.USB, native = true, discovery = true),
                capability(PrinterConnectionType.BLUETOOTH, native = true, discovery = true),
                capability(PrinterConnectionType.BLUETOOTH_LE, native = true, discovery = false),
                capability(PrinterConnectionType.VIRTUAL, native = true, discovery = true)
            ),
            notes = listOf("Bluetooth and USB permissions are required on supported Android versions.")
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.USB -> "Grant USB permission for the selected device before printing."
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> "Grant Bluetooth permissions and keep location/Bluetooth enabled when required by Android."
            PrinterConnectionType.NETWORK -> "Use the printer IP address and make sure the device is on the same network."
            else -> "No Android-specific troubleshooting hint available."
        }
    }

    actual fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return PrinterUsbDiagnostic(
            deviceFound = false,
            canOpen = false,
            canClaimInterface = false,
            failureReason = PrinterUsbFailureReason.UNSUPPORTED_PLATFORM,
            message = "Android USB uses Android UsbManager permission flow, not JVM libusb diagnostics.",
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
            message = "Android BLE diagnostics require runtime Bluetooth adapter/device inspection.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH_LE)
        )
    }

    private fun capability(type: String, native: Boolean, discovery: Boolean): PrinterTransportCapability {
        return PrinterTransportCapability(type, isSupported = true, isNative = native, supportsDiscovery = discovery)
    }
}
