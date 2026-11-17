package ngga.ring.printer

import kotlinx.datetime.TimeZone
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.model.PrinterProfile
import ngga.ring.printer.model.PrinterConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptServiceTest {

    private val receiptService = ReceiptService()

    @Test
    fun testFormatTimestampProducesNumericMonthAndDay() {
        // 2026-11-17T05:30:00Z in epoch millis: 1794893400000L
        val timestamp = 1794893400000L
        val formattedUtc = receiptService.formatTimestamp(timestamp, TimeZone.UTC)
        assertEquals("17-11-2026 05:30", formattedUtc)
    }

    @Test
    fun testGenerateTestPrintProducesNonEmptyWellFormedBytes() {
        val config = PrinterConfig(
            name = "TestPrinter",
            connection = PrinterConnection.NETWORK,
            profile = PrinterProfile.MM80
        )
        val bytes = receiptService.generateTestPrint(config)
        assertTrue(bytes.isNotEmpty(), "Test print bytes should not be empty")
        // Verify ESC @ (0x1B, 0x40) initialize command is at the start
        assertTrue(bytes.size > 10)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testGenerateTestPreviewProducesBlocks() {
        val config = PrinterConfig(
            name = "TestPrinter",
            connection = PrinterConnection.NETWORK,
            profile = PrinterProfile.MM80
        )
        val preview = receiptService.generateTestPreview(config)
        assertTrue(preview.isNotEmpty(), "Preview blocks should not be empty")
    }

    @Test
    fun testGenerateCalibrationReceiptProducesBytes() {
        val config = PrinterConfig(
            name = "TestPrinter",
            connection = PrinterConnection.NETWORK,
            profile = PrinterProfile.MM58
        )
        val bytes = receiptService.generateCalibrationReceipt(config)
        assertTrue(bytes.isNotEmpty())
    }
}
