package ngga.ring.printer.usecase

import ngga.ring.printer.manager.PrinterPlatformDiagnostics
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterUsbDiagnostic

class GetPrinterDiagnosticsUseCase(
    private val diagnostics: PrinterPlatformDiagnostics = PrinterPlatformDiagnostics()
) {
    fun report(): PrinterPlatformReport = diagnostics.getReport()

    fun troubleshootingHint(connectionType: String): String {
        return diagnostics.troubleshootingHint(connectionType)
    }

    fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return diagnostics.diagnoseUsb(config)
    }

    fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic {
        return diagnostics.diagnoseBle(config)
    }

    fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        return diagnostics.diagnoseSerial(config)
    }
}
