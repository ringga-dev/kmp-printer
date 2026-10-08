package ngga.ring.printer

import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterDevice
import ngga.ring.printer.model.PrinterDevices
import ngga.ring.printer.model.PrinterProfile
import ngga.ring.printer.model.toPrinterDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrinterDeviceTest {

    @Test
    fun testBluetoothDeviceMapping() {
        val device = PrinterDevices.bluetooth(
            name = "RPP02N",
            macAddress = "00:11:22:33:44:55",
            isBonded = true,
            baudRate = 115200
        )

        assertEquals("RPP02N", device.name)
        assertEquals("00:11:22:33:44:55", device.address)
        assertEquals(PrinterConnection.BLUETOOTH, device.connectionType)

        val config = device.toPrinterConfig(PrinterProfile.MM58)
        assertEquals(PrinterConnection.BLUETOOTH.value, config.connectionType)
        assertEquals("00:11:22:33:44:55", config.address)
        assertEquals(115200, config.baudRate)
        assertEquals(58, config.paperWidth)
        assertEquals(384, config.paperWidthDots)

        val transport = device.toTransportConfig()
        assertEquals("RPP02N", transport.name)
    }

    @Test
    fun testBleDeviceMapping() {
        val device = PrinterDevices.ble(
            name = "BLE-Printer",
            macOrUuid = "AA:BB:CC:DD:EE:FF",
            rssi = -65,
            serviceUuid = "0000ff00-0000-1000-8000-00805f9b34fb"
        )

        assertEquals(PrinterConnection.BLE, device.connectionType)
        assertEquals(-65, device.rssi)

        val config = device.toPrinterConfig(PrinterProfile.MM80)
        assertEquals(PrinterConnection.BLE.value, config.connectionType)
        assertEquals("AA:BB:CC:DD:EE:FF", config.address)
        assertEquals(80, config.paperWidth)
        assertEquals(576, config.paperWidthDots)
    }

    @Test
    fun testUsbDeviceMapping() {
        val device = PrinterDevices.usb(
            name = "Epson TM-T82",
            identifier = "USB_RAW:04B8:0202",
            vendorId = 0x04B8,
            productId = 0x0202
        )

        assertEquals(PrinterConnection.USB, device.connectionType)
        assertEquals("USB_RAW:04B8:0202", device.address)
        assertEquals(0x04B8, device.vendorId)

        val config = device.toPrinterConfig()
        assertEquals(PrinterConnection.USB.value, config.connectionType)
        assertEquals("USB_RAW:04B8:0202", config.address)
    }

    @Test
    fun testNetworkDeviceMapping() {
        val device = PrinterDevices.network(
            name = "Kitchen Printer",
            host = "192.168.1.150",
            port = 9100,
            timeoutMs = 3000
        )

        assertEquals(PrinterConnection.NETWORK, device.connectionType)
        assertEquals("192.168.1.150:9100", device.address)
        assertEquals("192.168.1.150", device.host)
        assertEquals(9100, device.port)

        val config = device.toPrinterConfig(PrinterProfile.MM80)
        assertEquals(PrinterConnection.NETWORK.value, config.connectionType)
        assertEquals("192.168.1.150", config.address)
        assertEquals(9100, config.port)
        assertEquals(3000, config.connectionTimeoutMs)
    }

    @Test
    fun testSerialDeviceMapping() {
        val device = PrinterDevices.serial(
            name = "COM3 Printer",
            portName = "COM3",
            baudRate = 19200
        )

        assertEquals(PrinterConnection.SERIAL, device.connectionType)
        val config = device.toPrinterConfig()
        assertEquals(PrinterConnection.SERIAL.value, config.connectionType)
        assertEquals("COM3", config.address)
        assertEquals(19200, config.baudRate)
    }

    @Test
    fun testVirtualDeviceMapping() {
        val device = PrinterDevices.virtual("Simulator")

        assertEquals(PrinterConnection.VIRTUAL, device.connectionType)
        val config = device.toPrinterConfig()
        assertEquals(PrinterConnection.VIRTUAL.value, config.connectionType)
    }

    @Test
    fun testDiscoveredPrinterConversion() {
        val discovered = DiscoveredPrinter(
            name = "Network Pos",
            connectionType = "NETWORK",
            address = "192.168.0.200",
            port = 9100
        )

        val device = discovered.toPrinterDevice()
        assertTrue(device is PrinterDevice.Network)
        assertEquals("Network Pos", device.name)
        assertEquals("192.168.0.200", device.host)
        assertEquals(9100, device.port)

        val backToDiscovered = device.toDiscoveredPrinter()
        assertEquals(discovered.name, backToDiscovered.name)
        assertEquals(discovered.connectionType, backToDiscovered.connectionType)
        assertEquals("192.168.0.200:9100", backToDiscovered.address)
    }
}
