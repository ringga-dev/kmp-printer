package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.preview.PreviewBlock
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.*
import ngga.ring.printer.util.escpos.ESCPosConfig

/**
 * Service for generating thermal printer receipt bytes.
 * Uses ESC/POS command builder for high compatibility across devices.
 */
class ReceiptService {


    /**
     * Generates a comprehensive hardware test page adapted to printer configuration.
     */
    fun generateTestPrint(config: ngga.ring.printer.model.PrinterConfig): ByteArray {
        return getTestBuilder(config).build()
    }

    /**
     * Generates a logical preview of the test page for UI display.
     */
    fun generateTestPreview(config: ngga.ring.printer.model.PrinterConfig): List<PreviewBlock> {
        return getTestBuilder(config).buildPreview()
    }

    fun generateCalibrationReceipt(config: ngga.ring.printer.model.PrinterConfig): ByteArray {
        // Determine safe maximum dots based on selected paper class
        val is80mm = config.paperWidth >= 80
        val maxDots = if (is80mm) 640 else 440
        val charsPerLine = if (is80mm) 64 else 42

        val builder = ESCPosCommandBuilder(
            ngga.ring.printer.util.escpos.ESCPosConfig(
                charsPerLine = charsPerLine,
                paperWidthDots = maxDots
            )
        ).initialize()

        builder.alignCenter().bold(true).line("HARDWARE CALIBRATION WIZARD").bold(false)
            .line("Paper Class Detector: ${if (is80mm) "80mm" else "58mm"}")
            .line("Look at the ruler below and identify the")
            .line("left-most and right-most visible dots.")
            .feed(1)
            .printRuler()
            .feed(1)
            .line("Common Limits Info:")
            .line("58mm paper: usually 384 dots")
            .line("80mm paper: usually 560-576 dots")
            .feedLines(3)
            .cut()

        return builder.build()
    }

    private fun getTestBuilder(config: ngga.ring.printer.model.PrinterConfig): ESCPosCommandBuilder {
        val builder = ESCPosCommandBuilder.fromPrinterConfig(config).initialize()
        
        // Print logo if available in config

        // Sample receipt data
        val storeName = "NGGA Store"
        val address = listOf("Jl. Example No. 123", "Jakarta, Indonesia")
        val orderNumber = "ORD-001234"
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val dateStr = formatTimestamp(timestamp)
        val items = listOf(
            Triple("Nasi Goreng", 2, 25000.0),
            Triple("Ayam Bakar", 1, 30000.0),
            Triple("Es Teh", 3, 5000.0),
            Triple("Kerupuk", 1, 10000.0),
            Triple("Sambal", 2, 2000.0),
            Triple("Ayam Goreng Crispy", 1, 35000.0),
            Triple("Jus Jeruk", 2, 8000.0),
            Triple("Nasi Putih", 1, 5000.0)
        )
        val subtotal = items.sumOf { it.second * it.third }
        val taxRate = 0.1
        val tax = subtotal * taxRate
        val total = subtotal + tax
        val payment = 150000.0
        val change = payment - total

        builder.alignCenter()
            .bold(true)
            .bigFont()
            .line(storeName)
            .normalFont()
            .bold(false)
            .underline(true)
            .line("Official Receipt")
            .underline(false)
            .invert(true)
            .line(" *** COPY *** ")
            .invert(false)

        for (addr in address) {
            builder.line(addr)
        }

        builder.divider('-')

        builder.alignLeft()
            .line("Order #: $orderNumber")
            .line("Date: $dateStr")
            .divider('-')
            
        builder.bold(true).line("Items:").bold(false)

        for ((name, qty, price) in items) {
            val subtotalItem = qty * price
            builder.segmentedLine("$name x$qty @ Rp ${price.toInt()}", "Rp ${subtotalItem.toInt()}")
        }

        builder.divider('-')

        builder.segmentedLine("Subtotal:", "Rp ${subtotal.toInt()}")
            .segmentedLine("Tax (10%):", "Rp ${tax.toInt()}")
            .segmentedLine("Total:", "Rp ${total.toInt()}")

        builder.divider('-')
            .segmentedLine("Payment: Cash", "Rp ${payment.toInt()}")
            .segmentedLine("Change:", "Rp ${change.toInt()}")
            .divider('-')
            
        builder.alignCenter()
            .line("Thank you for shopping!")
            .line("Please come again")

        // Add QR and barcode
        builder.feed(1)
            .qrCode("https://github.com/ringga-dev", size = 6)
            .feed(1)
            .barcode(orderNumber)
            .feed(1)
            
        return builder.feedLines(3).cut()
    }

    internal fun formatTimestamp(
        timestamp: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val ldt = instant.toLocalDateTime(timeZone)
            val day = ldt.day.toString().padStart(2, '0')
            val month = ldt.month.number.toString().padStart(2, '0')
            val year = ldt.year
            val hour = ldt.hour.toString().padStart(2, '0')
            val minute = ldt.minute.toString().padStart(2, '0')
            "$day-$month-$year $hour:$minute"
        } catch (_: Exception) {
            "Tgl: $timestamp"
        }
    }
}
