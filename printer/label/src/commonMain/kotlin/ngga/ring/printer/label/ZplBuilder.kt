package ngga.ring.printer.label

/**
 * [ZplBuilder] generates raw ZPL (Zebra Programming Language) commands
 * for Zebra-compatible label printers (Zebra, Godex, TSC, etc.).
 *
 * Output is a byte array ready to send over TCP or serial.
 *
 * Usage:
 * ```kotlin
 * val zpl = ZplBuilder().apply {
 *     labelSize = LabelSize.ZEBRA_4X6
 *     text("TOKO ABC", x = 50, y = 20, fontSize = FontSize.SIZE_4, bold = true)
 *     barcode("BRG-001", BarcodeFormat.CODE_128, x = 50, y = 120)
 *     qrCode("https://tokoku.id", x = 600, y = 120)
 *     price("25.000", x = 50, y = 400)
 *     text("Jl. Merdeka No.123", x = 50, y = 450, fontSize = FontSize.SIZE_0)
 * }.build()
 * // Send to printer: socket.write(zpl)
 * ```
 */
public class ZplBuilder {
    /** Label dimensions (default: 4×6 inch) */
    public var labelSize: LabelSize = LabelSize.defaultZebra

    private val elements = mutableListOf<LabelElement>()

    // ─── High-level element DSL ─────────────────────────

    /** Add a text element. */
    public fun text(
        text: String,
        x: Int = 20,
        y: Int = 10,
        fontSize: FontSize = FontSize.body,
        bold: Boolean = false,
        align: TextAlign = TextAlign.LEFT
    ) { elements.add(LabelElement.Text(text, x, y, fontSize, bold, align)) }

    /** Add a barcode (or QR). */
    public fun barcode(
        data: String,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        x: Int = 20,
        y: Int = 50,
        heightDots: Int = 100,
    ) { elements.add(LabelElement.Barcode(data, format, x, y, heightDots)) }

    /** Shorthand for QR codes. */
    public fun qrCode(
        data: String,
        x: Int = 20,
        y: Int = 50,
    ) { elements.add(LabelElement.Barcode(data, BarcodeFormat.QR, x, y)) }

    /** Horizontal separator line. */
    public fun separator(
        y: Int,
        x: Int = 10,
        width: Int = 800
    ) { elements.add(LabelElement.Separator(x, y, width)) }

    /** Vertical spacer. */
    public fun spacer(height: Int) { elements.add(LabelElement.Spacer(height)) }

    /** Price with currency prefix. */
    public fun price(
        amount: String,
        currency: String = "Rp",
        x: Int = 20,
        y: Int = 200,
        fontSize: FontSize = FontSize.SIZE_3,
        bold: Boolean = true
    ) { elements.add(LabelElement.Price(amount, currency, x, y, fontSize, bold)) }

    /** Set all elements at once. */
    public fun elements(elems: List<LabelElement>) { elements.addAll(elems) }

    /** Clear all elements. */
    public fun clear() { elements.clear() }

    // ─── Build ─────────────────────────────────────────

    /**
     * Generate the raw ZPL bytes.
     *
     * @return ZPL command string encoded as ASCII bytes.
     */
    public fun build(): ByteArray {
        val sb = StringBuilder()
        sb.append("^XA") // Start label

        // Label size
        val widthDots = (labelSize.labelWidthMm.toFloat() * 8).toInt() // 203 DPI ÷ 25.4 ≈ 8 dots/mm
        val heightDots = labelSize.labelHeightMm?.let { (it.toFloat() * 8).toInt() } ?: 1200
        sb.append("^LL$heightDots")
        sb.append("^PW$widthDots")
        sb.append("^CF0,20") // Default font

        if (labelSize.labelHeightMm != null) {
            sb.append("^FS") // Set label length
        }

        for (element in elements) {
            when (element) {
                is LabelElement.Text -> appendText(sb, element)
                is LabelElement.Barcode -> appendBarcode(sb, element)
                is LabelElement.Separator -> appendSeparator(sb, element)
                is LabelElement.Spacer -> { /* ZPL doesn't need explicit spacers */ }
                is LabelElement.Price -> appendPrice(sb, element)
            }
        }

        sb.append("^XZ") // End label
        return sb.toString().encodeToCharset()
    }

    // ─── Private helpers ───────────────────────────────

    private fun appendText(sb: StringBuilder, t: LabelElement.Text) {
        val alignCmd = when (t.align) {
            TextAlign.LEFT -> "N"
            TextAlign.CENTER -> "C"
            TextAlign.RIGHT -> "R"
        }
        val size = t.fontSize.dots
        val fontWeight = if (t.bold) "2" else "0"
        sb.append("^FO${t.x},${t.y}")
        sb.append("^A0$fontWeight,$size")
        sb.append("^FB${labelSize.labelWidthMm * 8},1,0,$alignCmd,0")
        sb.append("^FD${escapeZpl(t.text)}^FS")
    }

    private fun appendBarcode(sb: StringBuilder, b: LabelElement.Barcode) {
        when (b.format) {
            BarcodeFormat.CODE_128 -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BY3")
                sb.append("^BCN,${b.heightDots},Y,N,N")
                sb.append("^FD:${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.CODE_39 -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BY3")
                sb.append("^B3N,N,${b.heightDots},N,N")
                sb.append("^FD*${escapeZpl(b.data)}*^FS")
            }
            BarcodeFormat.EAN_13 -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BY3")
                sb.append("^BEN,${b.heightDots},N,N")
                sb.append("^FD${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.QR -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BQN,2,8")
                sb.append("^FDHM,${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.DATAMATRIX -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BXN,10,200")
                sb.append("^FD${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.PDF417 -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^B7N,${b.heightDots},5,,,,N")
                sb.append("^FD${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.ITF -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BY3")
                sb.append("^B2N,${b.heightDots},N,N,N")
                sb.append("^FD${escapeZpl(b.data)}^FS")
            }
            BarcodeFormat.UPC_A -> {
                sb.append("^FO${b.x},${b.y}")
                sb.append("^BY3")
                sb.append("^B5N,${b.heightDots},N,N")
                sb.append("^FD${escapeZpl(b.data)}^FS")
            }
        }
    }

    private fun appendSeparator(sb: StringBuilder, s: LabelElement.Separator) {
        // Draw a horizontal line using graphic box
        sb.append("^FO${s.x},${s.y}")
        sb.append("^GB${s.width},3,3^FS")
    }

    private fun appendPrice(sb: StringBuilder, p: LabelElement.Price) {
        val size = p.fontSize.dots
        val weight = if (p.bold) "2" else "0"
        // Currency
        sb.append("^FO${p.x},${p.y}")
        sb.append("^A0$weight,$size")
        sb.append("^FD${escapeZpl(p.currency)}^FS")
        // Amount — right-aligned for price
        val totalWidth = labelSize.labelWidthMm * 8
        val amountX = totalWidth - (p.amount.length * size) - 20
        sb.append("^FO${amountX},${p.y}")
        sb.append("^A0$weight,$size")
        sb.append("^FD${escapeZpl(p.amount)}^FS")
    }

    /** Escape ZPL special characters. */
    private fun escapeZpl(input: String): String =
        input.replace("\\", "\\\\")
            .replace("^", "\\^")
            .replace("~", "\\~")
            .replace("\"", "\\\"")
}
