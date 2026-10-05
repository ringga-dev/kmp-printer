package ngga.ring.printer.util.platform

/**
 * Pure Kotlin encoder for the character sets thermal printers actually use.
 *
 * The tables are decode tables borrowed from Java's `java.nio.charset.Charset`,
 * which is what the Android and JVM targets use at runtime, so output is
 * identical across all supported platforms.
 *
 * Character classes that need no table at all (ASCII, and ISO-8859-1 in its
 * first 128 positions) are handled inline; only the CJK and code page tables
 * are materialised, and each is built once on first use.
 */
internal object CharsetTables {

    /** Byte value substituted for characters the target charset cannot encode. */
    const val REPLACEMENT: Byte = 0x3F // '?'

    // ------------------------------------------------------------------ single byte

    private val singleByteIndex: Map<String, Map<Char, Byte>> by lazy {
        mapOf(
            "CP437" to CP437Table.TABLE.buildSingleByteIndex(),
            "ISO-8859-1" to ISO8859_1Table.TABLE.buildSingleByteIndex(),
            "WINDOWS-1252" to WINDOWS1252Table.TABLE.buildSingleByteIndex(),
        )
    }

    // ------------------------------------------------------------------ double byte

    private val gbkIndex: Map<Char, ByteArray> by lazy {
        GBKTable.lookupIndex()
    }

    private val big5Index: Map<Char, ByteArray> by lazy {
        BIG5Table.lookupIndex()
    }

    /**
     * Returns a lookup for [charsetName], or null when the charset is either
     * unknown or handled without a table.
     */
    fun singleByteLookup(charsetName: String): Map<Char, Byte>? =
        singleByteIndex[normalize(charsetName)]

    fun gbk(): Map<Char, ByteArray> = gbkIndex

    fun big5(): Map<Char, ByteArray> = big5Index

    fun hasDoubleByte(charsetName: String): Boolean = when (normalize(charsetName)) {
        "GBK", "GB2312", "GB18030", "CP936" -> true
        "BIG5", "BIG5-HKSCS", "CP950" -> true
        else -> false
    }

    private fun normalize(name: String): String = name.uppercase().trim()

    /**
     * Builds the reverse map for a single-byte table. Byte 0x00 is skipped so a
     * real U+0000 in the source text cannot resolve through the table.
     */
    private fun String.buildSingleByteIndex(): Map<Char, Byte> {
        val map = HashMap<Char, Byte>(length * 2)
        for (byteValue in indices) {
            val ch = this[byteValue]
            if (ch != '\u0000' && byteValue != 0) {
                // First mapping wins, mirroring how a decoder resolves the
                // lowest byte sequence for a given character. Written without
                // putIfAbsent because that helper is JVM-only in Kotlin.
                if (ch !in map) map[ch] = byteValue.toByte()
            }
        }
        return map
    }
}