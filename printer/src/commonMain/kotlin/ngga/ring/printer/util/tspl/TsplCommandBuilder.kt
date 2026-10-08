package ngga.ring.printer.util.tspl

/**
 * Pure Kotlin TSPL (TSC Printer Language) Command Builder for Label Printers.
 * Used for printing adhesive stickers, price tags, shipping labels, barcodes, and QR codes.
 */
class TsplCommandBuilder(
    val widthMm: Double = 40.0,
    val heightMm: Double = 30.0
) {
    private val commands = mutableListOf<String>()
    private val rawPayloads = mutableListOf<ByteArray>()

    init {
        size(widthMm, heightMm)
        gap(2.0, 0.0)
        cls()
    }

    /**
     * Sets label size in millimeters.
     */
    fun size(width: Double, height: Double): TsplCommandBuilder {
        commands.add("SIZE $width mm, $height mm")
        return this
    }

    /**
     * Sets gap distance between labels.
     */
    fun gap(distanceMm: Double = 2.0, offsetMm: Double = 0.0): TsplCommandBuilder {
        commands.add("GAP $distanceMm mm, $offsetMm mm")
        return this
    }

    /**
     * Sets print orientation (0 = normal, 1 = reversed 180°).
     */
    fun direction(direction: Int = 0): TsplCommandBuilder {
        commands.add("DIRECTION $direction")
        return this
    }

    /**
     * Clears the label image buffer.
     */
    fun cls(): TsplCommandBuilder {
        commands.add("CLS")
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
        commands.add("TEXT $x,$y,\"$font\",$rotation,$xMultiplication,$yMultiplication,\"$content\"")
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
        commands.add("BARCODE $x,$y,\"$type\",$height,$readable,$rotation,$narrow,$wide,\"$data\"")
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
        commands.add("QRCODE $x,$y,$eccLevel,$cellWidth,$mode,$rotation,\"$data\"")
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
        commands.add("BOX $xStart,$yStart,$xEnd,$yEnd,$thickness")
        return this
    }

    /**
     * Draws a horizontal/vertical line.
     */
    fun line(x: Int, y: Int, width: Int, height: Int): TsplCommandBuilder {
        commands.add("BAR $x,$y,$width,$height")
        return this
    }

    /**
     * Triggers the label cutter (if hardware cutter is installed).
     */
    fun cut(): TsplCommandBuilder {
        commands.add("CUT")
        return this
    }

    /**
     * Prints the label.
     * @param sets Number of sets to print (default 1).
     * @param copies Number of copies per set (default 1).
     */
    fun print(sets: Int = 1, copies: Int = 1): TsplCommandBuilder {
        commands.add("PRINT $sets,$copies")
        return this
    }

    /**
     * Generates all TSPL commands as a raw byte array terminated with CRLF.
     */
    fun build(): ByteArray {
        val sb = StringBuilder()
        for (cmd in commands) {
            sb.append(cmd).append("\r\n")
        }
        val textBytes = sb.toString().encodeToByteArray()
        if (rawPayloads.isEmpty()) return textBytes

        var totalSize = textBytes.size
        for (payload in rawPayloads) totalSize += payload.size

        val result = ByteArray(totalSize)
        textBytes.copyInto(result, 0)
        var offset = textBytes.size
        for (payload in rawPayloads) {
            payload.copyInto(result, offset)
            offset += payload.size
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
