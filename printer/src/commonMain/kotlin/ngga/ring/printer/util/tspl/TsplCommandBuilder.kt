package ngga.ring.printer.util.tspl

/**
 * Pure Kotlin TSPL (TSC Printer Language) Command Builder for Label Printers.
 * Used for printing adhesive stickers, price tags, shipping labels, barcodes, and QR codes.
 */
class TsplCommandBuilder(
    val widthMm: Double = 40.0,
    val heightMm: Double = 30.0
) {
    private val parts = mutableListOf<ByteArray>()
    private var hasPrint = false

    init {
        size(widthMm, heightMm)
        gap(2.0, 0.0)
        cls()
    }

    private fun appendCommand(cmd: String) {
        parts.add("$cmd\r\n".encodeToByteArray())
    }

    /**
     * Sets label size in millimeters.
     */
    fun size(width: Double, height: Double): TsplCommandBuilder {
        appendCommand("SIZE $width mm, $height mm")
        return this
    }

    /**
     * Sets gap distance between labels.
     */
    fun gap(distanceMm: Double = 2.0, offsetMm: Double = 0.0): TsplCommandBuilder {
        appendCommand("GAP $distanceMm mm, $offsetMm mm")
        return this
    }

    /**
     * Sets print orientation (0 = normal, 1 = reversed 180°).
     */
    fun direction(direction: Int = 0): TsplCommandBuilder {
        appendCommand("DIRECTION $direction")
        return this
    }

    /**
     * Clears the label image buffer.
     */
    fun cls(): TsplCommandBuilder {
        appendCommand("CLS")
        return this
    }

    /**
     * Draws text at coordinate (x, y) with specified font and multipliers.
     * Font: "1" (8x12), "2" (12x20), "3" (16x24), "4" (24x32), "5" (32x48), "TSS24.BF2" (Simplified Chinese), etc.
     */
    fun text(
        x: Int,
        y: Int,
        content: String,
        font: String = "3",
        rotation: Int = 0,
        xMultiplication: Int = 1,
        yMultiplication: Int = 1
    ): TsplCommandBuilder {
        appendCommand("TEXT $x,$y,\"$font\",$rotation,$xMultiplication,$yMultiplication,\"$content\"")
        return this
    }

    /**
     * Draws a 1D Barcode (e.g. "128", "EAN13", "39", "93", "UPCA").
     * @param readable 0: none, 1: human readable left, 2: human readable center, 3: human readable right.
     */
    fun barcode(
        x: Int,
        y: Int,
        data: String,
        type: String = "128",
        height: Int = 50,
        readable: Int = 2,
        rotation: Int = 0,
        narrow: Int = 2,
        wide: Int = 4
    ): TsplCommandBuilder {
        appendCommand("BARCODE $x,$y,\"$type\",$height,$readable,$rotation,$narrow,$wide,\"$data\"")
        return this
    }

    /**
     * Draws a 2D QR Code.
     * @param eccLevel L (7%), M (15%), Q (25%), H (30%)
     * @param cellWidth 1..10
     */
    fun qrcode(
        x: Int,
        y: Int,
        data: String,
        eccLevel: String = "M",
        cellWidth: Int = 4,
        mode: String = "A",
        rotation: Int = 0
    ): TsplCommandBuilder {
        appendCommand("QRCODE $x,$y,$eccLevel,$cellWidth,$mode,$rotation,\"$data\"")
        return this
    }

    /**
     * Draws a rectangular box with specified border thickness.
     */
    fun box(
        xStart: Int,
        yStart: Int,
        xEnd: Int,
        yEnd: Int,
        thickness: Int = 2
    ): TsplCommandBuilder {
        appendCommand("BOX $xStart,$yStart,$xEnd,$yEnd,$thickness")
        return this
    }

    /**
     * Draws a horizontal/vertical line.
     */
    fun line(x: Int, y: Int, width: Int, height: Int): TsplCommandBuilder {
        appendCommand("BAR $x,$y,$width,$height")
        return this
    }

    /**
     * Draws a monochrome bitmap graphic onto the label buffer.
     *
     * In TSPL standard:
     * Header: `BITMAP X,Y,width,height,mode,` followed immediately by [data] byte array, followed by `\r\n`.
     *
     * @param x X-coordinate in dots
     * @param y Y-coordinate in dots
     * @param widthBytes Bitmap width in bytes (dots / 8)
     * @param heightDots Bitmap height in dots
     * @param mode 0 = OVERWRITE, 1 = OR, 2 = XOR
     * @param data Raw 1-bit monochrome bitmap byte array
     */
    fun bitmap(
        x: Int,
        y: Int,
        widthBytes: Int,
        heightDots: Int,
        mode: Int = 0,
        data: ByteArray
    ): TsplCommandBuilder {
        parts.add("BITMAP $x,$y,$widthBytes,$heightDots,$mode,".encodeToByteArray())
        parts.add(data)
        parts.add("\r\n".encodeToByteArray())
        return this
    }

    /**
     * Triggers the label cutter (if hardware cutter is installed).
     */
    fun cut(): TsplCommandBuilder {
        appendCommand("CUT")
        return this
    }

    /**
     * Prints the label.
     * @param sets Number of sets to print (default 1).
     * @param copies Number of copies per set (default 1).
     */
    fun print(sets: Int = 1, copies: Int = 1): TsplCommandBuilder {
        hasPrint = true
        appendCommand("PRINT $sets,$copies")
        return this
    }

    /**
     * Generates all TSPL commands as a raw byte array terminated with CRLF.
     * If [print] has not been explicitly called, automatically appends `PRINT 1,1`.
     */
    fun build(): ByteArray {
        if (!hasPrint) {
            appendCommand("PRINT 1,1")
            hasPrint = true
        }

        val totalSize = parts.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (part in parts) {
            part.copyInto(result, offset)
            offset += part.size
        }
        return result
    }

    companion object {
        fun label(
            widthMm: Double = 40.0,
            heightMm: Double = 30.0,
            block: TsplCommandBuilder.() -> Unit
        ): TsplCommandBuilder {
            val builder = TsplCommandBuilder(widthMm, heightMm)
            builder.block()
            return builder
        }
    }
}
