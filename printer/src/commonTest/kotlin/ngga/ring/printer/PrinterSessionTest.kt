package ngga.ring.printer

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterDevices
import ngga.ring.printer.session.DefaultPrinterSession
import kotlin.test.Test
import kotlin.test.assertTrue

class PrinterSessionTest {

    @Test
    fun testOpenVirtualSessionAndPrint() = runTest {
        val printer = KmpPrinter()
        val virtualDevice = PrinterDevices.virtual("Cashier-1")
        val session = printer.openSession(virtualDevice)

        val statuses = mutableListOf<PrintStatus>()
        session.print {
            line("TEST RECEIPT")
            divider('-')
            segmentedLine("TOTAL", "Rp 10.000")
        }.toList(statuses)

        assertTrue(statuses.any { it is PrintStatus.Success })
        session.disconnect()
    }

    @Test
    fun testOpenCashDrawerSendsCorrectBytes() = runTest {
        val connector = RecordingConnector()
        val config = PrinterConfig(name = "TestCashDrawer", connection = PrinterConnection.VIRTUAL)
        val session = DefaultPrinterSession(config, connector)

        val resultPin0 = session.openCashDrawer(pin = 0)
        assertTrue(resultPin0)
        val expectedPin0 = byteArrayOf(0x1B, 0x70, 0x00, 0x32, 0xFF.toByte())
        assertTrue(connector.sentBytes.last().contentEquals(expectedPin0))

        val resultPin1 = session.openCashDrawer(pin = 1)
        assertTrue(resultPin1)
        val expectedPin1 = byteArrayOf(0x1B, 0x70, 0x01, 0x32, 0xFF.toByte())
        assertTrue(connector.sentBytes.last().contentEquals(expectedPin1))
    }

    @Test
    fun testBeepSendsCorrectBytes() = runTest {
        val connector = RecordingConnector()
        val config = PrinterConfig(name = "TestBeep", connection = PrinterConnection.VIRTUAL)
        val session = DefaultPrinterSession(config, connector)

        val resultDefault = session.beep()
        assertTrue(resultDefault)
        val expectedDefault = byteArrayOf(0x1B, 0x28, 0x41, 0x02, 0x00, 0x01, 0x01)
        assertTrue(connector.sentBytes.last().contentEquals(expectedDefault))

        val resultCustom = session.beep(count = 3, duration = 4)
        assertTrue(resultCustom)
        val expectedCustom = byteArrayOf(0x1B, 0x28, 0x41, 0x02, 0x00, 0x03, 0x04)
        assertTrue(connector.sentBytes.last().contentEquals(expectedCustom))
    }

    @Test
    fun testKmpPrinterCashDrawerAndBeepDirectShortcuts() = runTest {
        val printer = KmpPrinter()
        val virtualDevice = PrinterDevices.virtual("Simulator-1")

        val drawerStatuses = printer.openCashDrawer(virtualDevice).toList()
        assertTrue(drawerStatuses.any { it is PrintStatus.Success })

        val beepStatuses = printer.beep(virtualDevice, count = 2, duration = 1).toList()
        assertTrue(beepStatuses.any { it is PrintStatus.Success })
    }
}

private class RecordingConnector : PrinterConnector {
    val sentBytes = mutableListOf<ByteArray>()
    private var connected = false

    override suspend fun connect(config: PrinterConfig): Boolean {
        connected = true
        return true
    }

    override suspend fun sendData(data: ByteArray): Boolean {
        sentBytes.add(data)
        return true
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() {
        connected = false
    }

    override fun isConnected(): Boolean = connected
}

