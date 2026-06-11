package ngga.ring.printer.manager

import com.fazecast.jSerialComm.SerialPort
import ngga.ring.printer.model.PrinterConnectionType

interface JvmOsPrinterBackend {
    val os: JvmOperatingSystem

    fun listSerialPorts(): List<JvmSerialPortInfo>

    fun connectionHint(type: String): String
}

abstract class BaseJvmOsPrinterBackend(
    override val os: JvmOperatingSystem
) : JvmOsPrinterBackend {
    override fun listSerialPorts(): List<JvmSerialPortInfo> {
        return SerialPort.getCommPorts().map { port ->
            val descriptiveName = port.descriptivePortName ?: port.systemPortName
            val lowerName = descriptiveName.lowercase()
            JvmSerialPortInfo(
                name = descriptiveName,
                address = port.systemPortName,
                descriptiveName = descriptiveName,
                looksLikePrinter = lowerName.contains("printer") ||
                    lowerName.contains("esc") ||
                    lowerName.contains("pos"),
                looksLikeBluetooth = lowerName.contains("bluetooth") ||
                    lowerName.contains("standard serial over bluetooth") ||
                    lowerName.contains("bth") ||
                    lowerName.contains("rfcomm")
            )
        }
    }

    protected fun genericHint(type: String): String {
        return when (PrinterConnectionType.normalize(type)) {
            PrinterConnectionType.NETWORK -> "Use printer IP and raw TCP port, usually 9100."
            PrinterConnectionType.USB -> "JVM USB uses raw libusb first, then serial-port devices, then an installed OS printer queue if available."
            PrinterConnectionType.SERIAL -> "Use the OS serial device name shown by discovery."
            PrinterConnectionType.BLUETOOTH -> "Pair Bluetooth Classic/SPP in the OS first; JVM then uses the assigned serial port or OS printer queue."
            PrinterConnectionType.BLUETOOTH_LE -> "Native JVM BLE is not enabled yet; only serial-like BLE ports can work."
            else -> "No platform-specific hint available."
        }
    }
}

class WindowsJvmPrinterBackend : BaseJvmOsPrinterBackend(JvmOperatingSystem.WINDOWS) {
    override fun connectionHint(type: String): String {
        return when (PrinterConnectionType.normalize(type)) {
            PrinterConnectionType.USB -> "Windows USB uses raw WinUSB/libusb first, then COMx devices or an installed printer queue."
            PrinterConnectionType.BLUETOOTH -> "Windows Bluetooth Classic works through an outgoing COM port or an installed Bluetooth printer queue."
            PrinterConnectionType.BLUETOOTH_LE -> "Windows BLE needs a future WinRT backend for native BLE printing."
            else -> genericHint(type)
        }
    }
}

class LinuxJvmPrinterBackend : BaseJvmOsPrinterBackend(JvmOperatingSystem.LINUX) {
    override fun connectionHint(type: String): String {
        return when (PrinterConnectionType.normalize(type)) {
            PrinterConnectionType.USB,
            PrinterConnectionType.SERIAL -> "Linux serial printing needs permission for /dev/ttyUSB*, /dev/ttyACM*, or /dev/rfcomm*; CUPS queues can also be used."
            PrinterConnectionType.BLUETOOTH -> "Linux Bluetooth Classic can use rfcomm serial binding or an installed CUPS printer queue."
            PrinterConnectionType.BLUETOOTH_LE -> "Linux BLE needs a future BlueZ D-Bus backend for native BLE printing."
            else -> genericHint(type)
        }
    }
}

class MacosJvmPrinterBackend : BaseJvmOsPrinterBackend(JvmOperatingSystem.MACOS) {
    override fun connectionHint(type: String): String {
        return when (PrinterConnectionType.normalize(type)) {
            PrinterConnectionType.USB,
            PrinterConnectionType.SERIAL -> "macOS serial printing needs a visible /dev/tty.* or /dev/cu.* device, or an installed printer queue."
            PrinterConnectionType.BLUETOOTH -> "macOS Bluetooth Classic support depends on whether the OS exposes a serial device or printer queue."
            PrinterConnectionType.BLUETOOTH_LE -> "macOS BLE needs a future CoreBluetooth backend for native BLE printing."
            else -> genericHint(type)
        }
    }
}

class GenericJvmPrinterBackend : BaseJvmOsPrinterBackend(JvmOperatingSystem.OTHER) {
    override fun connectionHint(type: String): String = genericHint(type)
}
