package ngga.ring.printer.util.platform

import ngga.ring.printer.model.PrinterCharset
import ngga.ring.printer.util.PrinterLogger

/**
 * Encodes text to the byte sequence a thermal printer expects for a given
 * character set.
 *
 * Written in pure Kotlin so the result is identical on every platform. This
 * replaces the previous per-platform encoders, where the JS and iOS
 * implementations silently fell back to UTF-8 for GBK, CP437, WINDOWS-1252 and
 * BIG5 and produced unreadable output on the printer.
 */
object CharsetEncoder {

    /**
     * Encodes [text] into [charset].
     *
     * Characters the charset cannot represent become `?` rather than being
     * silently re-encoded, so the output is always printable, never corrupt.
     */
    fun encode(text: String, charset: PrinterCharset): ByteArray =
        encode(text, charset.value)

    fun encode(text: String, charsetName: String): ByteArray {
        if (text.isEmpty()) return ByteArray(0)

        val name = charsetName.uppercase().trim()

        // UTF-8 is identical to Kotlin's own encoder and needs no table.
        if (name == "UTF-8" || name == "UTF8") return text.encodeToByteArray()

        val doubleByte = when (name) {
            "GBK", "GB2312", "GB18030", "CP936" -> CharsetTables.gbk()
            "BIG5", "BIG5-HKSCS", "CP950" -> CharsetTables.big5()
            else -> null
        }
        if (doubleByte != null) return encodeDoubleByte(text, doubleByte)

        // US-ASCII keeps only the low 7 bits.
        if (name == "US-ASCII" || name == "ASCII") return encodeAscii(text)

        val singleByte = CharsetTables.singleByteLookup(name)
        if (singleByte != null) return encodeSingleByte(text, singleByte)

        // Unknown charset: UTF-8 keeps the text readable instead of degrading
        // every non-ASCII character to '?'. Logged because it is almost always a
        // typo in the caller's config rather than an intentional choice.
        PrinterLogger.warn(TAG, "Unknown charset '$charsetName', falling back to UTF-8")
        return text.encodeToByteArray()
    }

    private fun encodeAscii(text: String): ByteArray =
        ByteArray(text.length) { index ->
            val code = text[index].code
            if (code <= 0x7F) code.toByte() else CharsetTables.REPLACEMENT
        }

    private fun encodeSingleByte(text: String, table: Map<Char, Byte>): ByteArray =
        ByteArray(text.length) { index ->
            table[text[index]] ?: CharsetTables.REPLACEMENT
        }

    private fun encodeDoubleByte(text: String, table: Map<Char, ByteArray>): ByteArray {
        // Worst case two bytes per character, which is what GBK and BIG5 need.
        val out = ByteArray(text.length * 2)
        var size = 0
        for (ch in text) {
            // Both GBK and BIG5 keep ASCII in the single-byte range, and the
            // generated tables only cover lead bytes above 0x7F.
            val code = ch.code
            if (code <= 0x7F) {
                out[size++] = code.toByte()
                continue
            }
            val mapped = table[ch]
            if (mapped != null) {
                out[size++] = mapped[0]
                out[size++] = mapped[1]
            } else {
                out[size++] = CharsetTables.REPLACEMENT
            }
        }
        return if (size == out.size) out else out.copyOf(size)
    }

    private const val TAG = "CharsetEncoder"
}