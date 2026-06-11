package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterBleFailureReason
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterTransportCapability
import ngga.ring.printer.model.PrinterUsbDiagnostic
import ngga.ring.printer.model.PrinterUsbFailureReason
import platform.UIKit.UIDevice

actual class PrinterPlatformDiagnostics actual constructor() {
    actual fun getReport(): PrinterPlatformReport {
        return PrinterPlatformReport(
            platformName = "iOS",
            osName = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}",
            capabilities = listOf(
                capability(PrinterConnectionType.NETWORK, supported = true, native = true, discovery = false),
                capability(PrinterConnectionType.BLUETOOTH_LE, supported = true, native = true, discovery = true),
                capability(PrinterConnectionType.BLUETOOTH, supported = true, native = false, discovery = true),
                capability(PrinterConnectionType.USB, supported = false, native = false, discovery = false),
                capability(PrinterConnectionType.VIRTUAL, supported = true, native = true, discovery = true)
            ),
            notes = listOf("iOS Bluetooth printing is implemented through CoreBluetooth/BLE.")
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> "Make sure the printer exposes a writable BLE characteristic."
            PrinterConnectionType.NETWORK -> "Use a reachable IPv4 address and raw TCP port."
            PrinterConnectionType.USB -> "USB printing is not available in this iOS backend."
            else -> "No iOS-specific troubleshooting hint available."
        }
    }

    actual fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return PrinterUsbDiagnostic(
            deviceFound = false,
            canOpen = false,
            canClaimInterface = false,
            failureReason = PrinterUsbFailureReason.UNSUPPORTED_PLATFORM,
            message = "iOS raw USB diagnostics are not available in this backend.",
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
            message = "iOS BLE diagnostics require CoreBluetooth scan/connect flow.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH_LE)
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
