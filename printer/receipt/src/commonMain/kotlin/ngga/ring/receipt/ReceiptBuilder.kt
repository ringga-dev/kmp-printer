package ngga.ring.receipt

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

/**
 * DSL-based receipt builder that generates ESC/POS byte commands
 * for thermal receipt printers (Epson, Star, etc.).
 *
 * This is a high-level wrapper around [ESCPosCommandBuilder]
 * designed for quick receipt templating.
 *
 * Usage:
 * ```kotlin
 * val bytes = ReceiptBuilder().apply {
 *     storeName("TOKO ABC")
 *     address("Jl. Merdeka No. 123")
 *     separator()
 *     lineItem("Kopi Hitam", 2, 15000)
 *     lineItem("Nasi Goreng", 1, 25000)
 *     separator()
 *     total("Rp 55.000")
 *     payment("Tunai", "Rp 100.000")
 *     change("Rp 45.000")
 *     qrCode("order-123-abc")
 *     lineFeeds(3)
 *     cut()
 * }.build()
 * ```
 */
public class ReceiptBuilder {

    private val commandBuilder = ESCPosCommandBuilder()

    // ─── Receipt elements DSL ────────────────────────

    /** Set receipt store/brand header. */
    public fun storeName(name: String) {
        commandBuilder.initialize()
        commandBuilder.setAlignCenter()
        commandBuilder.setBold(true)
        commandBuilder.setFontSizeDouble()
        commandBuilder.addText(name)
        commandBuilder.lineFeed()
        commandBuilder.setBold(false)
        commandBuilder.setFontSizeNormal()
    }

    /** Add address / subtitle line (centred). */
    public fun address(line: String) {
        commandBuilder.setAlignCenter()
        commandBuilder.addText(line)
        commandBuilder.lineFeed()
    }

    /** Print a horizontally centred date/time line. */
    public fun dateTime(text: String) {
        commandBuilder.setAlignCenter()
        commandBuilder.addText(text)
        commandBuilder.lineFeed()
    }

    /** Draw a separator line across the receipt. */
    public fun separator() {
        commandBuilder.setAlignCenter()
        commandBuilder.addText("=".repeat(32))
        commandBuilder.lineFeed()
    }

    /** Dashed separator. */
    public fun dashedSeparator() {
        commandBuilder.setAlignCenter()
        commandBuilder.addText("-".repeat(32))
        commandBuilder.lineFeed()
    }

    /**
     * Add a single line item with qty × price.
     * Supports variable column widths.
     */
    public fun lineItem(name: String, qty: Int, price: Long, discount: Long = 0) {
        commandBuilder.setAlignLeft()
        val discountText = if (discount > 0) "  (disc: Rp $discount)" else ""
        val subTotal = (price * qty) - discount
        commandBuilder.addText("$name${discountText}")
        commandBuilder.lineFeed()

        val qtyStr = "$qty x Rp ${format(price)}"
        val totalStr = "Rp ${format(subTotal)}"

        commandBuilder.addText("  $qtyStr")
        // Right-align the total
        val padding = 32 - qtyStr.length - totalStr.length
        if (padding > 0) commandBuilder.addText(" ".repeat(padding))
        commandBuilder.addText(totalStr)
        commandBuilder.lineFeed()
    }

    /** Print a divider-less item line (simple format). */
    public fun simpleItem(name: String, value: String) {
        val padding = 32 - name.length - value.length
        commandBuilder.setAlignLeft()
        commandBuilder.addText(name)
        if (padding > 0) commandBuilder.addText(" ".repeat(padding))
        commandBuilder.addText(value)
        commandBuilder.lineFeed()
    }

    /** Add a total row (bold, larger font). */
    public fun total(text: String) {
        commandBuilder.setBold(true)
        commandBuilder.setFontSizeDouble()
        commandBuilder.setAlignLeft()
        commandBuilder.addText("TOTAL")
        val padding = 22
        commandBuilder.addText(" ".repeat(padding))
        commandBuilder.addText(text)
        commandBuilder.lineFeed()
        commandBuilder.setFontSizeNormal()
        commandBuilder.setBold(false)
    }

    /** Payment method line. */
    public fun payment(method: String, amount: String) {
        commandBuilder.setAlignLeft()
        commandBuilder.addText("$method: $amount")
        commandBuilder.lineFeed()
    }

    /** Change due line. */
    public fun change(amount: String) {
        commandBuilder.setBold(true)
        commandBuilder.addText("KEMBALI: $amount")
        commandBuilder.lineFeed()
        commandBuilder.setBold(false)
    }

    /** Print a QR code on the receipt. */
    public fun qrCode(data: String) {
        commandBuilder.setAlignCenter()
        commandBuilder.addQRCode(data)
        commandBuilder.lineFeed()
    }

    /** Print a barcode on the receipt. */
    public fun barcode(data: String) {
        commandBuilder.setAlignCenter()
        commandBuilder.addBarcode(data)
        commandBuilder.lineFeed()
    }

    /** Add a simple text line (centred or left-aligned). */
    public fun textLine(line: String, center: Boolean = false, bold: Boolean = false) {
        if (center) commandBuilder.setAlignCenter()
        else commandBuilder.setAlignLeft()
        if (bold) commandBuilder.setBold(true)
        commandBuilder.addText(line)
        commandBuilder.lineFeed()
        if (bold) commandBuilder.setBold(false)
    }

    /** Feed N blank lines. */
    public fun lineFeeds(n: Int) {
        repeat(n) { commandBuilder.lineFeed() }
    }

    /** Cut receipt paper. */
    public fun cut() {
        commandBuilder.cut()
    }

    /**
     * Open cash drawer (pin 2 — most common).
     */
    public fun openDrawer() {
        commandBuilder.openCashDrawer()
    }

    // ─── Build ───────────────────────────────────────

    /** Generate the complete ESC/POS byte array. */
    public fun build(): ByteArray {
        return commandBuilder.build()
    }

    // ─── Utility ────────────────────────────────────

    private fun format(amount: Long): String {
        val s = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.lastIndex downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, '.')
            sb.insert(0, s[i])
            count++
        }
        return sb.toString()
    }
}
