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
            platformName = "WasmJS Browser",
            osName = "Browser",
            capabilities = listOf(
                PrinterTransportCapability(PrinterConnectionType.VIRTUAL, true, true, true),
                PrinterTransportCapability(PrinterConnectionType.USB, false, false, false),
                PrinterTransportCapability(PrinterConnectionType.BLUETOOTH_LE, false, false, false),
                PrinterTransportCapability(PrinterConnectionType.NETWORK, false, false, false)
            ),
            notes = listOf("WasmJS currently supports virtual printing only in the library factory.")
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return "WasmJS hardware printing needs a JS bridge or browser API integration."
    }

    actual fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return PrinterUsbDiagnostic(
            deviceFound = false,
            canOpen = false,
            canClaimInterface = false,
            failureReason = PrinterUsbFailureReason.UNSUPPORTED_PLATFORM,
            message = "WasmJS raw USB diagnostics are not available in this backend.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.USB)
        )
    }

    actual fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic {
        return PrinterBleDiagnostic(
            adapterAvailable = false,
            deviceFound = false,
            serviceFound = false,
            writableCharacteristicFound = false,
            failureReason = PrinterBleFailureReason.UNSUPPORTED_PLATFORM,
            message = "WasmJS BLE diagnostics are not available in this backend.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH_LE)
        )
    }

    actual fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        return PrinterSerialDiagnostic(
            portFound = false,
            canOpen = false,
            canWrite = false,
            failureReason = PrinterSerialFailureReason.UNSUPPORTED_PLATFORM,
            message = "WasmJS cannot inspect OS Bluetooth Classic SPP serial ports.",
            suggestedFix = troubleshootingHint(PrinterConnectionType.BLUETOOTH)
        )
    }
}
