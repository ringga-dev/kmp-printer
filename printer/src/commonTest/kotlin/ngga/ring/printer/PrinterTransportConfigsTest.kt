package ngga.ring.printer

import ngga.ring.printer.model.BluetoothClassicPrinterConfig
import ngga.ring.printer.model.BlePrinterConfig
import ngga.ring.printer.model.NetworkPrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.UsbPrinterConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrinterTransportConfigsTest {
    @Test
    fun mapsTypedNetworkConfigToLegacyConfig() {
        val config = NetworkPrinterConfig("Kitchen", "192.168.1.50", 9100).toPrinterConfig()

        assertEquals(PrinterConnectionType.NETWORK, config.connectionType)
        assertEquals("192.168.1.50", config.address)
        assertEquals(9100, config.port)
    }

    @Test
    fun mapsTypedUsbConfigToLegacyConfig() {
        val config = UsbPrinterConfig("USB", "USB_RAW:04B8:0202").toPrinterConfig()

        assertEquals(PrinterConnectionType.USB, config.connectionType)
        assertEquals("USB_RAW:04B8:0202", config.address)
    }

    @Test
    fun mapsTypedBluetoothClassicConfigToLegacyConfig() {
        val config = BluetoothClassicPrinterConfig("BT", "AA:BB:CC:DD:EE:FF").toPrinterConfig()

        assertEquals(PrinterConnectionType.BLUETOOTH, config.connectionType)
        assertTrue(config.bluetoothClassicAutoBind)
    }

    @Test
    fun mapsTypedBleConfigToLegacyConfig() {
        val config = BlePrinterConfig("BLE", "AA:BB:CC:DD:EE:FF").toPrinterConfig()

        assertEquals(PrinterConnectionType.BLUETOOTH_LE, config.connectionType)
        assertTrue(config.bleAutoDiscover)
    }
}
