package ngga.ring.label

/**
 * [BrotherQlBuilder] generates Brother QL Raster Command Set bytes
 * for Brother QL-series label printers (QL-500, QL-600, QL-700, QL-800, QL-1060N, etc.).
 *
 * Brother QL printers work with continuous tape (die-cut labels or continuous rolls)
 * and receive raster image data after a set of configuration commands.
 *
 * Output is a byte array ready to send over TCP/IP, USB, or Bluetooth serial.
 *
 * Usage:
 * ```kotlin
 * val data = BrotherQlBuilder().apply {
 *     labelSize = LabelSize.QL_62
 *     text("TOKO ABC", x = 10, y = 10, bold = true)
 *     barcode("BRG-001", BarcodeFormat.CODE_128, y = 60)
 *     separator(y = 120)
 *     price("25.000", y = 140)
 * }.build()
 * // Send to printer: socket.write(data)
 * ```
 */
public class BrotherQlBuilder {
    /** Label tape width (default: 62mm wide Brother tape). */
    public var labelSize: LabelSize = LabelSize.defaultBrother

    private val elements = mutableListOf<LabelElement>()

    // ─── High-level element DSL ─────────────────────────

    public fun text(
        text: String,
        x: Int = 10,
        y: Int = 10,
        fontSize: FontSize = FontSize.body,
        bold: Boolean = false,
        align: TextAlign = TextAlign.LEFT
    ) { elements.add(LabelElement.Text(text, x, y, fontSize, bold, align)) }

    public fun barcode(
        data: String,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        x: Int = 10,
        y: Int = 50,
        heightDots: Int = 100
    ) { elements.add(LabelElement.Barcode(data, format, x, y, heightDots)) }

    public fun qrCode(
        data: String,
        x: Int = 10,
        y: Int = 50
    ) { elements.add(LabelElement.Barcode(data, BarcodeFormat.QR, x, y)) }

    public fun separator(
        y: Int,
        x: Int = 5,
        width: Int = 480
    ) { elements.add(LabelElement.Separator(x, y, width)) }

    public fun spacer(height: Int) { elements.add(LabelElement.Spacer(height)) }

    public fun price(
        amount: String,
        currency: String = "Rp",
        x: Int = 10,
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
     * Generate the Brother QL Raster Command Set bytes.
     *
     * The output includes:
     * 1. Status request / initialise
     * 2. Media / tape size command
     * 3. Raster graphics data lines
     * 4. Print command
     * 5. Form feed
     */
    public fun build(): ByteArray {
        val out = mutableListOf<Byte>()

        // 1. Initialise
        out.add(0x1B) // ESC
        out.add(0x40) // @ — Initialize

        // 2. Status request (optional — printer responds with status)
        out.add(0x1B) // ESC
        out.add('i'.code.toByte())
        out.add('S'.code.toByte())

        // 3. Tape width / media size
        val tapeWidthMm = labelSize.labelWidthMm
        val tapeCmd = tapeWidthToBytes(tapeWidthMm)
        // Inform the printer of media size
        out.addAll(tapeCmd.toList())

        // 4. Raster graphics — render elements as monochrome bitmap
        val rasterWidthDots = (tapeWidthMm.toFloat() * 8).toInt() // ≈8 dots/mm at 203 DPI
        val rasterHeightDots = estimateRasterHeight()

        // Convert elements to 1-bit raster data
        val rasterData = renderToRaster(rasterWidthDots, rasterHeightDots)

        // Write raster data in Brother QL format
        for (row in rasterData) {
            // Raster line command: 'g' + rowData
            out.add(0x47) // 'G' — Raster graphics transfer
            out.addAll(row.toList())
        }

        // 5. Print
        out.add(0x0A) // LF
        out.add(0x1A) // Ctrl+Z — Print / End

        return out.toByteArray()
    }

    // ─── Private helpers ───────────────────────────────

    /**
     * Convert tape width to Brother media size command bytes.
     */
    private fun tapeWidthToBytes(mm: Int): ByteArray {
        val tapeId = when (mm) {
            12 -> 0x0B
            29 -> 0x0C
            38 -> 0x0D
            50 -> 0x0E
            54 -> 0x0F
            62 -> 0x10
            102 -> 0x11
            else -> 0x10 // default 62mm
        }
        return byteArrayOf(0x1B, 0x69, 0x7A, tapeId.toByte())
    }

    /**
     * Estimate the number of raster lines needed.
     */
    private fun estimateRasterHeight(): Int {
        var maxY = 200 // minimum
        for (element in elements) {
            val yEnd = when (element) {
                is LabelElement.Text -> element.y + element.fontSize.dots
                is LabelElement.Barcode -> element.y + element.heightDots
                is LabelElement.Separator -> element.y + 5
                is LabelElement.Spacer -> element.y + element.height
                is LabelElement.Price -> element.y + element.fontSize.dots + 5
            }
            if (yEnd > maxY) maxY = yEnd
        }
        // Add some padding at the bottom
        return maxY + 50
    }

    /**
     * Render all label elements into a monochrome (1-bit) raster image,
     * one row at a time. Each row is a byte array where each bit represents
     * one dot (1 = black, 0 = white).
     *
     * Brother QL uses a column-based raster format where each byte
     * represents 8 vertical dots (MSB at top).
     */
    private fun renderToRaster(widthDots: Int, heightDots: Int): List<ByteArray> {
        val rows = mutableListOf<ByteArray>()
        val bytesPerRow = (widthDots + 7) / 8

        for (y in 0 until heightDots) {
            val row = ByteArray(bytesPerRow) { 0 }
            for (element in elements) {
                when (element) {
                    is LabelElement.Text -> renderTextRow(row, element, y, widthDots, bytesPerRow)
                    is LabelElement.Barcode -> renderBarcodeRow(row, element, y, widthDots, bytesPerRow)
                    is LabelElement.Separator -> renderSeparatorRow(row, element, y, widthDots, bytesPerRow)
                    is LabelElement.Spacer -> { /* skip — no content */ }
                    is LabelElement.Price -> renderPriceRow(row, element, y, widthDots, bytesPerRow)
                }
            }
            rows.add(row)
        }
        return rows
    }

    /**
     * Render a single row of text at [y].
     */
    private fun renderTextRow(
        row: ByteArray,
        t: LabelElement.Text,
        y: Int,
        widthDots: Int,
        _bytesPerRow: Int
    ) {
        if (y < t.y || y >= t.y + t.fontSize.dots) return
        val charIndex = (y - t.y) / t.fontSize.dots
        if (charIndex >= t.text.length) return
        val char = t.text[charIndex]
        // Simple monochrome character rendering (8×8 dot matrix)
        val charBitmap = getSimpleFont(char)
        if (charBitmap == null) return

        val xOffset = t.x
        for (cx in 0 until 8.coerceAtMost(widthDots - xOffset)) {
            if (cx < charBitmap.size && (charBitmap[cx] shr (7 - (y - t.y) % t.fontSize.dots).toInt() and 1) == 1) {
                val byteIdx = (xOffset + cx) / 8
                val bitIdx = (xOffset + cx) % 8
                if (byteIdx < row.size) {
                    row[byteIdx] = (row[byteIdx].toInt() or (1 shl (7 - bitIdx))).toByte()
                }
            }
        }
    }

    /**
     * Render a simple 8×8 font for Brother QL text.
     * Returns a bitmap (8 bytes, MSB = top row) for common ASCII chars.
     */
    private fun getSimpleFont(char: Char): ByteArray? {
        if (char.code < 32 || char.code > 126) return null
        return SIMPLE_FONT[char.code - 32]
    }

    /**
     * Minimal 8×8 pixel font bitmap (8 bytes per char, MSB-first by column).
     * Row 0 = top of character.
     */
    companion object {
        private val SIMPLE_FONT: Array<ByteArray> by lazy {
            val font = Array(95) { ByteArray(8) { 0 } }
            // ' ' (space)
            // '0' .. '9' - simple 7-segment style
            val digits = listOf(
                byteArrayOf(0x3E, 0x51, 0x49, 0x45, 0x3E, 0, 0, 0), // 0
                byteArrayOf(0x00, 0x42, 0x7F, 0x40, 0x00, 0, 0, 0), // 1
                byteArrayOf(0x42, 0x61, 0x51, 0x49, 0x46, 0, 0, 0), // 2
                byteArrayOf(0x21, 0x41, 0x45, 0x4B, 0x31, 0, 0, 0), // 3
                byteArrayOf(0x18, 0x14, 0x12, 0x7F, 0x10, 0, 0, 0), // 4
                byteArrayOf(0x27, 0x45, 0x45, 0x45, 0x39, 0, 0, 0), // 5
                byteArrayOf(0x3C, 0x4A, 0x49, 0x49, 0x30, 0, 0, 0), // 6
                byteArrayOf(0x01, 0x71, 0x09, 0x05, 0x03, 0, 0, 0), // 7
                byteArrayOf(0x36, 0x49, 0x49, 0x49, 0x36, 0, 0, 0), // 8
                byteArrayOf(0x06, 0x49, 0x49, 0x29, 0x1E, 0, 0, 0), // 9
            )
            digits.forEachIndexed { i, b -> font[i + 16] = b } // '0' = index 16

            // Basic uppercase letters (A-Z)
            val letters = listOf(
                byteArrayOf(0x7E, 0x09, 0x09, 0x09, 0x7E, 0, 0, 0), // A
                byteArrayOf(0x7F, 0x49, 0x49, 0x49, 0x36, 0, 0, 0), // B
                byteArrayOf(0x3E, 0x41, 0x41, 0x41, 0x22, 0, 0, 0), // C
                byteArrayOf(0x7F, 0x41, 0x41, 0x22, 0x1C, 0, 0, 0), // D
                byteArrayOf(0x7F, 0x49, 0x49, 0x49, 0x41, 0, 0, 0), // E
                byteArrayOf(0x7F, 0x09, 0x09, 0x09, 0x01, 0, 0, 0), // F
                byteArrayOf(0x3E, 0x41, 0x49, 0x49, 0x7A, 0, 0, 0), // G
                byteArrayOf(0x7F, 0x08, 0x08, 0x08, 0x7F, 0, 0, 0), // H
                byteArrayOf(0x00, 0x41, 0x7F, 0x41, 0x00, 0, 0, 0), // I
                byteArrayOf(0x20, 0x40, 0x41, 0x3F, 0x01, 0, 0, 0), // J
                byteArrayOf(0x7F, 0x08, 0x14, 0x22, 0x41, 0, 0, 0), // K
                byteArrayOf(0x7F, 0x40, 0x40, 0x40, 0x40, 0, 0, 0), // L
                byteArrayOf(0x7F, 0x02, 0x04, 0x02, 0x7F, 0, 0, 0), // M
                byteArrayOf(0x7F, 0x04, 0x08, 0x10, 0x7F, 0, 0, 0), // N
                byteArrayOf(0x3E, 0x41, 0x41, 0x41, 0x3E, 0, 0, 0), // O
                byteArrayOf(0x7F, 0x09, 0x09, 0x09, 0x06, 0, 0, 0), // P
                byteArrayOf(0x3E, 0x41, 0x51, 0x21, 0x5E, 0, 0, 0), // Q
                byteArrayOf(0x7F, 0x09, 0x19, 0x29, 0x46, 0, 0, 0), // R
                byteArrayOf(0x46, 0x49, 0x49, 0x49, 0x31, 0, 0, 0), // S
                byteArrayOf(0x01, 0x01, 0x7F, 0x01, 0x01, 0, 0, 0), // T
                byteArrayOf(0x3F, 0x40, 0x40, 0x40, 0x3F, 0, 0, 0), // U
                byteArrayOf(0x07, 0x18, 0x60, 0x18, 0x07, 0, 0, 0), // V
                byteArrayOf(0x1F, 0x60, 0x1C, 0x60, 0x1F, 0, 0, 0), // W
                byteArrayOf(0x63, 0x14, 0x08, 0x14, 0x63, 0, 0, 0), // X
                byteArrayOf(0x03, 0x04, 0x78, 0x04, 0x03, 0, 0, 0), // Y
                byteArrayOf(0x61, 0x51, 0x49, 0x45, 0x43, 0, 0, 0), // Z
            )
            letters.forEachIndexed { i, b -> font[i + 33] = b } // 'A' = index 33

            // Common symbols
            font[32] = byteArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0, 0, 0) // . (full stop)
            font[12] = byteArrayOf(0x08, 0x08, 0x3E, 0x08, 0x08, 0, 0, 0) // +

            font
        }
    }

    // ─── Barcode rendering (simplified) ────────────────

    private fun renderBarcodeRow(
        row: ByteArray,
        b: LabelElement.Barcode,
        y: Int,
        widthDots: Int,
        _bytesPerRow: Int
    ) {
        if (y < b.y || y >= b.y + b.heightDots) return

        // Simple barcode: vertical bars at fixed positions
        val barWidth = 3 // 3 dots per bar
        val barStep = 12 // 12 dots between bar centers
        val startX = b.x

        for (i in b.data.indices) {
            val barX = startX + i * barStep
            val isBlack = (b.data[i].code % 2 == 1)

            if (isBlack && barX < widthDots - barWidth) {
                for (dx in 0 until barWidth) {
                    val px = barX + dx
                    val byteIdx = px / 8
                    val bitIdx = px % 8
                    if (byteIdx < row.size) {
                        row[byteIdx] = (row[byteIdx].toInt() or (1 shl (7 - bitIdx))).toByte()
                    }
                }
            }
        }
    }

    // ─── Separator rendering ──────────────────────────

    private fun renderSeparatorRow(
        row: ByteArray,
        s: LabelElement.Separator,
        y: Int,
        _widthDots: Int,
        _bytesPerRow: Int
    ) {
        if (y != s.y) return

        val endX = (s.x + s.width).coerceAtMost(row.size * 8 - 1)
        for (px in s.x..endX) {
            val byteIdx = px / 8
            val bitIdx = px % 8
            if (byteIdx < row.size) {
                row[byteIdx] = (row[byteIdx].toInt() or (1 shl (7 - bitIdx))).toByte()
            }
        }
    }

    // ─── Price rendering ──────────────────────────────

    private fun renderPriceRow(
        row: ByteArray,
        p: LabelElement.Price,
        y: Int,
        _widthDots: Int,
        _bytesPerRow: Int
    ) {
        if (y < p.y || y >= p.y + p.fontSize.dots) return
        // Price = currency string rendered as text at p.x, amount at p.x + offset
        val text = "${p.currency} ${p.amount}"
        val charIndex = (y - p.y) / p.fontSize.dots
        if (charIndex >= text.length) return
        val char = text[charIndex]
        val charBitmap = getSimpleFont(char) ?: return

        val xOffset = p.x + charIndex * 10
        for (cx in 0 until 8.coerceAtMost(row.size * 8 - xOffset)) {
            if (cx < charBitmap.size && (charBitmap[cx] shr (7 - (y - p.y) % p.fontSize.dots).toInt() and 1) == 1) {
                val byteIdx = (xOffset + cx) / 8
                val bitIdx = (xOffset + cx) % 8
                if (byteIdx < row.size) {
                    row[byteIdx] = (row[byteIdx].toInt() or (1 shl (7 - bitIdx))).toByte()
                }
            }
        }
    }
}
