package ngga.ring.printer

import kotlinx.coroutines.test.runTest
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.manager.WebBluetoothConnector
import ngga.ring.printer.manager.WebSerialConnector
import ngga.ring.printer.manager.WebUsbConnector
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Browser-specific factory routing.
 *
 * The JS transport mapping is the one part of the factory that differs from
 * Android and JVM, so it gets coverage on top of the shared tests.
 *
 * `connect()` is deliberately not exercised: `requestPort()` only resolves from
 * a real user gesture against a real printer, so awaiting it in a headless
 * browser would hang rather than fail fast. Everything below the prompt is
 * covered instead.
 */
class WebPrinterFactoryTest {

    private val factory = PrinterConnectorFactory()

    private fun config(type: String) = PrinterConfig(
        name = "Web Printer",
        connectionType = type,
        address = "127.0.0.1",
        port = 9100,
    )

    @Test
    fun bluetoothMapsToWebBluetoothConnector() {
        assertTrue(
            factory.create(config(PrinterConnectionType.BLUETOOTH)) is WebBluetoothConnector,
        )
        assertTrue(
            factory.create(config(PrinterConnectionType.BLUETOOTH_LE)) is WebBluetoothConnector,
            "BLE must share the Web Bluetooth transport",
        )
    }

    @Test
    fun usbMapsToWebUsbConnector() {
        assertTrue(factory.create(config(PrinterConnectionType.USB)) is WebUsbConnector)
    }

    @Test
    fun serialMapsToWebSerialConnector() {
        // Previously fell through to a stub that always returned false.
        assertTrue(factory.create(config(PrinterConnectionType.SERIAL)) is WebSerialConnector)
    }

    @Test
    fun virtualTransportDoesNotMapToAWebConnector() {
        assertFalse(factory.create(config(PrinterConnectionType.VIRTUAL)) is WebUsbConnector)
    }

    @Test
    fun networkIsUnsupportedAndReportsFailureNotCrash() = runTest {
        // Browsers cannot open raw TCP sockets, so the connector must report
        // this cleanly rather than throwing.
        val connector = factory.create(config(PrinterConnectionType.NETWORK))

        assertFalse(connector.isConnected())
        assertFalse(connector.sendData(byteArrayOf(0x1B)))
        assertNull(connector.readData(4, timeout = 50))
        connector.disconnect()
    }

    @Test
    fun serialConnectorFailsFastBeforeAPortIsChosen() = runTest {
        val connector = WebSerialConnector()

        assertFalse(connector.isConnected())
        // No writer is held, so writes must return false instead of throwing.
        assertFalse(connector.sendData(byteArrayOf(0x1B)))
        assertNull(connector.readData(4, timeout = 50))
    }

    @Test
    fun serialConnectorToleratesRepeatedDisconnect() = runTest {
        val connector = WebSerialConnector()
        connector.disconnect()
        connector.disconnect()
        assertFalse(connector.isConnected())
    }

    @Test
    fun webConnectorsStartDisconnected() {
        // Fresh instances must never claim to be connected, otherwise the UI
        // would offer to print before a device was chosen.
        assertFalse(WebSerialConnector().isConnected())
        assertFalse(WebUsbConnector().isConnected())
        assertFalse(WebBluetoothConnector().isConnected())
    }
}