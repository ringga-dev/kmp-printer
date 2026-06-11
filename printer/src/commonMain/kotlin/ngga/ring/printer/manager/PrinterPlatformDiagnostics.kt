package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterUsbDiagnostic

expect class PrinterPlatformDiagnostics {
    constructor()

    fun getReport(): PrinterPlatformReport

    fun troubleshootingHint(connectionType: String): String

    fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic

    fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic

    fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic
}
