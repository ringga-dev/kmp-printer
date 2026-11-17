package ngga.ring.printer.util.preview

import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.platform.CharsetTableSingleByte

/**
 * ESC/POS Virtual Renderer — Parses raw ESC/POS byte arrays into a list of VirtualLines.
 * This enables developers to preview receipts without wasting paper.
 *
 * Supported commands:
 * - ESC @ (Initialize)
 * - ESC a n (Alignment)
 * - ESC E n (Bold on/off)
 * - ESC - n (Underline on/off)
 * - GS B n (Invert on/off)
 * - GS ! n (Text size)
 * - GS v 0 (Raster image — placeholder)
 * - GS ( k (QR code — placeholder)
 * - GS k (Barcode — placeholder)
 * - GS V (Cut)
 * - LF (Line feed)
 */
object ESCPosVirtualRenderer {

    /**
     * Parses raw ESC/POS bytes and returns a visual representation.
     *
     * @param data The raw ESC/POS byte array.
     * @param charsPerLine Maximum characters per line for formatting (default 32).
     * @return List of VirtualLine representing the visual output.
     */
    fun render(data: ByteArray, charsPerLine: Int = 32): List<VirtualLine> {
        val lines = mutableListOf<VirtualLine>()
        var currentLine = StringBuilder()

        // State tracking
        var isBold = false
        var isUnderline = false
        var isInverted = false
        var alignment = TextAlignment.LEFT
        var widthMultiplier = 1
        var heightMultiplier = 1
        var activeCodePage: Byte = 0

        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF

            when (b) {
                // LF — Flush current line
                0x0A -> {
                    lines.add(VirtualLine(
                        content = currentLine.toString(),
                        type = if (currentLine.isEmpty()) LineType.SPACE else LineType.TEXT,
                        alignment = alignment,
                        isBold = isBold,
                        isUnderline = isUnderline,
                        isInverted = isInverted,
                        widthMultiplier = widthMultiplier,
                        heightMultiplier = heightMultiplier
                    ))
                    currentLine = StringBuilder()
                }

                // ESC sequence
                0x1B -> {
                    if (i + 1 >= data.size) { i++; continue }
                    val next = data[++i].toInt() and 0xFF
                    when (next) {
                        // ESC @ — Initialize
                        0x40 -> {
                            isBold = false
                            isUnderline = false
                            isInverted = false
                            alignment = TextAlignment.LEFT
                            widthMultiplier = 1
                            heightMultiplier = 1
                            activeCodePage = 0
                        }
                        // ESC a n — Alignment
                        0x61 -> {
                            if (i + 1 < data.size) {
                                alignment = when (data[++i].toInt() and 0xFF) {
                                    1 -> TextAlignment.CENTER
                                    2 -> TextAlignment.RIGHT
                                    else -> TextAlignment.LEFT
                                }
                            }
                        }
                        // ESC E n — Bold
                        0x45 -> {
                            if (i + 1 < data.size) isBold = (data[++i].toInt() and 0xFF) != 0
                        }
                        // ESC - n — Underline
                        0x2D -> {
                            if (i + 1 < data.size) isUnderline = (data[++i].toInt() and 0xFF) != 0
                        }
                        // ESC t n — Code page
                        0x74 -> {
                            if (i + 1 < data.size) {
                                activeCodePage = data[++i]
                            }
                        }
                        // ESC ( A — Beep (skip pL pH n1 n2)
                        0x28 -> {
                            if (i + 1 < data.size) {
                                val sub = data[i + 1].toInt() and 0xFF
                                if (sub == 0x41) { // Beep
                                    i += 5 // Skip pL pH n1 n2
                                }
                            }
                        }
                        // ESC p — Cash drawer (skip 4 bytes)
                        0x70 -> { i += 3 }
                        // ESC 3 n — Line spacing (skip)
                        0x33 -> { if (i + 1 < data.size) i++ }
                        // ESC 2 — Reset line spacing (no params)
                        0x32 -> { /* no-op */ }
                        // ESC L — Enter page mode (skip for preview)
                        0x4C -> { /* no-op */ }
                        // ESC S — Exit page mode
                        0x53 -> { /* no-op */ }
                        // ESC W — Set print area in page mode (skip 8 bytes)
                        0x57 -> { i += 8 }
                        // ESC T n — Set page direction (skip)
                        0x54 -> { if (i + 1 < data.size) i++ }
                        // ESC $ — Horizontal position (skip 2 bytes)
                        0x24 -> { i += 2 }
                        // ESC FF — Print page and return
                        0x0C -> { /* no-op */ }
                    }
                }

                // GS sequence
                0x1D -> {
                    if (i + 1 >= data.size) { i++; continue }
                    val next = data[++i].toInt() and 0xFF
                    when (next) {
                        // GS ! n — Text size
                        0x21 -> {
                            if (i + 1 < data.size) {
                                val size = data[++i].toInt() and 0xFF
                                widthMultiplier = ((size shr 4) and 0x07) + 1
                                heightMultiplier = (size and 0x07) + 1
                                // Skip re-alignment byte that follows
                                if (i + 2 < data.size && data[i + 1].toInt() and 0xFF == 0x1B && data[i + 2].toInt() and 0xFF == 0x61) {
                                    i += 2 // skip ESC a
                                    if (i + 1 < data.size) i++ // skip alignment value
                                }
                            }
                        }
                        // GS B n — Invert
                        0x42 -> {
                            if (i + 1 < data.size) isInverted = (data[++i].toInt() and 0xFF) != 0
                        }
                        // GS V n — Cut
                        0x56 -> {
                            if (i + 1 < data.size) i++ // skip mode
                            lines.add(VirtualLine(content = "--- CUT ---", type = LineType.CUT, alignment = TextAlignment.CENTER))
                        }
                        // GS v 0 — Raster image
                        0x76 -> {
                            if (i + 1 < data.size && data[i + 1].toInt() and 0xFF == 0x30) {
                                i++ // skip 0x30
                                if (i + 4 < data.size) {
                                    val xL = data[++i].toInt() and 0xFF
                                    val xH = data[++i].toInt() and 0xFF
                                    val yL = data[++i].toInt() and 0xFF
                                    val yH = data[++i].toInt() and 0xFF
                                    val widthBytes = xL + (xH shl 8)
                                    val height = yL + (yH shl 8)
                                    val imageDataSize = widthBytes * height
                                    i += imageDataSize // skip image data
                                    lines.add(VirtualLine(
                                        content = "[IMAGE ${widthBytes * 8}x${height}]",
                                        type = LineType.IMAGE,
                                        alignment = alignment
                                    ))
                                }
                            }
                        }
                        // GS ( k — QR Code or PDF417 or DataMatrix
                        0x28 -> {
                            if (i + 1 < data.size && data[i + 1].toInt() and 0xFF == 0x6B) {
                                i++ // skip 0x6B
                                if (i + 2 < data.size) {
                                    val pL = data[++i].toInt() and 0xFF
                                    val pH = data[++i].toInt() and 0xFF
                                    val paramLen = pL + (pH shl 8)
                                    // Check if this is a QR print command (31 51 30)
                                    if (paramLen == 3 && i + 3 < data.size) {
                                        val fn1 = data[i + 1].toInt() and 0xFF
                                        val fn2 = data[i + 2].toInt() and 0xFF
                                        val fn3 = data[i + 3].toInt() and 0xFF
                                        if (fn1 == 0x31 && fn2 == 0x51 && fn3 == 0x30) {
                                            lines.add(VirtualLine(content = "[QR CODE]", type = LineType.QR_CODE, alignment = alignment))
                                        }
                                    }
                                    i += paramLen // skip params
                                }
                            }
                        }
                        // GS k — Barcode
                        0x6B -> {
                            if (i + 1 < data.size) {
                                val barcodeType = data[++i].toInt() and 0xFF
                                if (barcodeType >= 65) {
                                    // System B: next byte is length
                                    if (i + 1 < data.size) {
                                        val len = data[++i].toInt() and 0xFF
                                        val barcodeData = StringBuilder()
                                        for (j in 0 until len) {
                                            if (i + 1 < data.size) {
                                                barcodeData.append((data[++i].toInt() and 0xFF).toChar())
                                            }
                                        }
                                        lines.add(VirtualLine(content = "[BARCODE: $barcodeData]", type = LineType.BARCODE, alignment = alignment))
                                    }
                                }
                            }
                        }
                        // GS h n — Barcode height (skip)
                        0x68 -> { if (i + 1 < data.size) i++ }
                        // GS w n — Barcode width (skip)
                        0x77 -> { if (i + 1 < data.size) i++ }
                        // GS L — Left margin (skip 2 bytes)
                        0x4C -> { i += 2 }
                        // GS W — Printable area width (skip 2 bytes)
                        0x57 -> { i += 2 }
                        // GS $ — Vertical position (skip 2 bytes)
                        0x24 -> { i += 2 }
                    }
                }

                // FS sequence (NV Graphics)
                0x1C -> {
                    if (i + 1 >= data.size) { i++; continue }
                    val next = data[++i].toInt() and 0xFF
                    when (next) {
                        // FS p — Print NV image
                        0x70 -> {
                            if (i + 2 < data.size) {
                                val n = data[++i].toInt() and 0xFF
                                val mode = data[++i].toInt() and 0xFF
                                lines.add(VirtualLine(content = "[NV IMAGE #$n mode=$mode]", type = LineType.IMAGE, alignment = alignment))
                            }
                        }
                    }
                }

                // DLE sequence (status query — skip)
                0x10 -> {
                    if (i + 2 < data.size) i += 2
                }

                // Printable ASCII
                in 0x20..0x7E -> {
                    currentLine.append(b.toChar())
                }

                // Extended ASCII (0x80-0xFF)
                in 0x80..0xFF -> {
                    val decodedChar = decodeExtendedAscii(b, activeCodePage)
                    currentLine.append(decodedChar)
                }
            }
            i++
        }

        // Flush remaining content
        if (currentLine.isNotEmpty()) {
            lines.add(VirtualLine(
                content = currentLine.toString(),
                type = LineType.TEXT,
                alignment = alignment,
                isBold = isBold,
                isUnderline = isUnderline,
                isInverted = isInverted,
                widthMultiplier = widthMultiplier,
                heightMultiplier = heightMultiplier
            ))
        }

        return lines
    }

    /**
     * Renders the virtual lines into a simple ASCII art string for console/log preview.
     */
    fun toAsciiArt(lines: List<VirtualLine>, paperWidth: Int = 32): String {
        return buildString {
            appendLine("┌${"─".repeat(paperWidth)}┐")
            for (line in lines) {
                val content = when (line.type) {
                    LineType.CUT -> "✂${"─".repeat(paperWidth - 1)}"
                    LineType.SPACE -> ""
                    else -> {
                        val text = line.content.take(paperWidth)
                        when (line.alignment) {
                            TextAlignment.CENTER -> {
                                val pad = ((paperWidth - text.length) / 2).coerceAtLeast(0)
                                " ".repeat(pad) + text
                            }
                            TextAlignment.RIGHT -> text.padStart(paperWidth)
                            else -> text
                        }
                    }
                }
                val padded = content.padEnd(paperWidth).take(paperWidth)
                appendLine("│$padded│")
            }
            appendLine("└${"─".repeat(paperWidth)}┘")
        }
    }

    internal fun decodeExtendedAscii(byteVal: Int, codePage: Byte): Char {
        val table = when (codePage.toInt() and 0xFF) {
            0 -> CharsetTableSingleByte.CP437_DECODE
            1 -> CharsetTableSingleByte.ISO_8859_1_DECODE
            16 -> CharsetTableSingleByte.WINDOWS_1252_DECODE
            else -> null
        }
        if (table != null) {
            if (byteVal in 0 until table.length) {
                val ch = table[byteVal]
                if (ch != '\u0000' && ch != '\ufffd') return ch
            }
            return '?'
        }
        // Fallback for unknown code page: try CP437 table, otherwise byteVal.toChar()
        if (byteVal in 0 until CharsetTableSingleByte.CP437_DECODE.length) {
            val ch = CharsetTableSingleByte.CP437_DECODE[byteVal]
            if (ch != '\u0000' && ch != '\ufffd') return ch
        }
        return if (byteVal in 0x80..0xFF) byteVal.toChar() else '?'
    }
}
