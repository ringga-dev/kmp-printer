package ngga.ring.printer

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterProfileTest {
    @Test
    fun maps58mmProfileToPrinterConfig() {
        val config = PrinterConfig(
            name = "Kitchen",
            connection = PrinterConnection.NETWORK,
            profile = PrinterProfile.MM58,
            address = "192.168.1.50"
        )

        assertEquals(PrinterConnectionType.NETWORK, config.connectionType)
        assertEquals(58, config.paperWidth)
        assertEquals(384, config.paperWidthDots)
        assertEquals(32, config.characterPerLine)
    }

    @Test
    fun maps80mmProfileToPrinterConfig() {
        val config = PrinterConfig(
            name = "Cashier",
            connection = PrinterConnection.USB,
            profile = PrinterProfile.MM80,
            address = "USB_RAW:04B8:0202"
        )

        assertEquals(PrinterConnectionType.USB, config.connectionType)
        assertEquals(80, config.paperWidth)
        assertEquals(576, config.paperWidthDots)
        assertEquals(48, config.characterPerLine)
    }

    @Test
    fun bleAliasNormalizesToBluetoothLe() {
        assertEquals(PrinterConnectionType.BLUETOOTH_LE, PrinterConnection.BLE.value)
        assertEquals(PrinterConnectionType.BLUETOOTH_LE, PrinterConnectionType.normalize(PrinterConnection.BLE))
        assertEquals(PrinterConnection.BLE, PrinterConnection.from("ble"))
    }
}

