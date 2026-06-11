package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterBleDiagnostic
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterPlatformReport
import ngga.ring.printer.model.PrinterSerialDiagnostic
import ngga.ring.printer.model.PrinterTransportCapability
import ngga.ring.printer.model.PrinterUsbDiagnostic

actual class PrinterPlatformDiagnostics actual constructor() {
    private val portService = JvmPrinterPortService()
    private val usbService = JvmUsbDeviceService()
    private val bleService = JvmBleService()

    actual fun getReport(): PrinterPlatformReport {
        val os = portService.currentOs()
        return PrinterPlatformReport(
            platformName = "JVM",
            osName = os.name,
            capabilities = listOf(
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.NETWORK,
                    isSupported = true,
                    isNative = true,
                    supportsDiscovery = true,
                    notes = listOf("Raw TCP/IP printing is available on all JVM OS backends.")
                ),
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.SERIAL,
                    isSupported = true,
                    isNative = true,
                    supportsDiscovery = true,
                    notes = listOf("Serial printing uses jSerialComm and the OS device name.")
                ),
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.USB,
                    isSupported = true,
                    isNative = true,
                    supportsDiscovery = true,
                    notes = listOf(
                        "USB tries raw libusb access first, then serial-port devices, then OS printer queues.",
                        portService.rawUsbHint()
                    )
                ),
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.BLUETOOTH,
                    isSupported = true,
                    isNative = false,
                    supportsDiscovery = true,
                    notes = listOf(
                        "Bluetooth Classic now ranks OS-specific targets first: outgoing COM on Windows, rfcomm or tty on Linux, and /dev/cu.* on macOS.",
                        "If no serial port is exposed, the JVM falls back to the installed printer queue."
                    )
                ),
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.BLUETOOTH_LE,
                    isSupported = false,
                    isNative = false,
                    supportsDiscovery = true,
                    notes = listOf(
                        "Native BLE backend is structured per OS but not implemented yet.",
                        bleService.troubleshootingHint(),
                        "Serial-like BLE ports may still appear on some systems."
                    )
                ),
                PrinterTransportCapability(
                    connectionType = PrinterConnectionType.VIRTUAL,
                    isSupported = true,
                    isNative = true,
                    supportsDiscovery = true,
                    notes = listOf("Virtual printing is available for tests and previews.")
                )
            ),
            notes = listOf(
                portService.connectionHint(PrinterConnectionType.USB),
                portService.connectionHint(PrinterConnectionType.BLUETOOTH_LE),
                bleService.troubleshootingHint()
            )
        )
    }

    actual fun troubleshootingHint(connectionType: String): String {
        return portService.connectionHint(PrinterConnectionType.normalize(connectionType))
    }

    actual fun diagnoseUsb(config: PrinterConfig): PrinterUsbDiagnostic {
        return usbService.diagnose(config)
    }

    actual fun diagnoseBle(config: PrinterConfig): PrinterBleDiagnostic {
        return bleService.diagnose(config)
    }

    actual fun diagnoseSerial(config: PrinterConfig): PrinterSerialDiagnostic {
        return portService.diagnoseSerial(config)
    }
}
