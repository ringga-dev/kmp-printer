package ngga.ring.printer

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterDevices
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
}
