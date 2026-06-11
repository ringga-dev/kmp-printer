package ngga.ring.printer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ngga.ring.printer.model.PrinterConnectionType

class PrinterConnectionTypeTest {
    @Test
    fun normalizesLegacyAliases() {
        assertEquals(PrinterConnectionType.NETWORK, PrinterConnectionType.normalize("tcp"))
        assertEquals(PrinterConnectionType.SERIAL, PrinterConnectionType.normalize("usb_serial"))
        assertEquals(PrinterConnectionType.BLUETOOTH, PrinterConnectionType.normalize("bt"))
        assertEquals(PrinterConnectionType.BLUETOOTH_LE, PrinterConnectionType.normalize("ble"))
    }

    @Test
    fun identifiesJvmSerialBackedTransports() {
        assertTrue(PrinterConnectionType.usesSerialPortOnJvm(PrinterConnectionType.SERIAL))
        assertTrue(PrinterConnectionType.usesSerialPortOnJvm(PrinterConnectionType.USB))
        assertTrue(PrinterConnectionType.usesSerialPortOnJvm(PrinterConnectionType.BLUETOOTH))
        assertTrue(PrinterConnectionType.usesSerialPortOnJvm(PrinterConnectionType.BLUETOOTH_LE))
        assertFalse(PrinterConnectionType.usesSerialPortOnJvm(PrinterConnectionType.NETWORK))
    }
}
