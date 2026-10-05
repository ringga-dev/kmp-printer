package ngga.ring.printer

import ngga.ring.printer.model.PrinterCharset
import ngga.ring.printer.util.platform.CharsetEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the expected byte output for each supported charset.
 *
 * These vectors are the same ones Java's `Charset` produces, so a failure here
 * means the shared tables have drifted from the Android and JVM behaviour.
 */
class CharsetEncoderTest {

    private fun encode(text: String, charset: PrinterCharset): List<Byte> =
        CharsetEncoder.encode(text, charset).toList()

    @Test
    fun asciiIsIdenticalAcrossEveryCharset() {
        val ascii = "KMP Printer 123"
        // Built by hand rather than with String.encodeToByteArray, which is not
        // available on every KMP target.
        val expected = ascii.map { it.code.toByte() }
        for (charset in PrinterCharset.entries) {
            assertContentEquals(
                expected,
                encode(ascii, charset),
                "ASCII must survive ${charset.value}",
            )
        }
    }

    @Test
    fun emptyTextProducesEmptyOutput() {
        for (charset in PrinterCharset.entries) {
            assertEquals(0, CharsetEncoder.encode("", charset).size)
        }
    }

    @Test
    fun utf8UsesStandardEncoding() {
        // 'é' is U+00E9, which UTF-8 encodes as C3 A9.
        assertContentEquals(
            listOf(0x61, 0xC3.toByte(), 0xA9.toByte()),
            encode("aé", PrinterCharset.UTF8),
        )
    }

    @Test
    fun iso88591MapsHighLatinDirectly() {
        // 0xE9 is 'é' in both ISO-8859-1 and Windows-1252.
        assertContentEquals(listOf(0xE9.toByte()), encode("é", PrinterCharset.ISO8859_1))
        assertContentEquals(listOf(0xE9.toByte()), encode("é", PrinterCharset.WINDOWS_1252))
    }

    @Test
    fun windows1252MapsCurrenciesThatIsoCannotRepresent() {
        // Windows-1252 assigns 0x80..0x9F to symbols; ISO-8859-1 treats those
        // as control codes, so '€' must only encode on Windows-1252.
        assertContentEquals(listOf(0x80.toByte()), encode("€", PrinterCharset.WINDOWS_1252))
        assertContentEquals(listOf(0x3F), encode("€", PrinterCharset.ISO8859_1))
    }

    @Test
    fun cp437MapsBoxDrawingAndBlockCharacters() {
        // CP437 0xB0 is '░' (light shade) and 0xBA is '║' (double vertical).
        assertContentEquals(listOf(0xB0.toByte()), encode("░", PrinterCharset.CP437))
        assertContentEquals(listOf(0xBA.toByte()), encode("║", PrinterCharset.CP437))
    }

    @Test
    fun cp437ReplacesCharactersOutsideItsRange() {
        // '€' has no CP437 mapping, so it must degrade to '?'.
        assertContentEquals(listOf(0x3F), encode("€", PrinterCharset.CP437))
    }

    @Test
    fun gbkEncodesCjkAsTwoBytes() {
        val encoded = CharsetEncoder.encode("中", PrinterCharset.GBK)
        assertEquals(2, encoded.size, "GBK uses two bytes per CJK character")
        assertContentEquals(listOf(0xD6.toByte(), 0xD0.toByte()), encoded.toList())
    }

    @Test
    fun gbkKeepsAsciiAsSingleBytesAlongsideCjk() {
        val encoded = CharsetEncoder.encode("A中B", PrinterCharset.GBK).toList()
        assertContentEquals(
            listOf(0x41, 0xD6.toByte(), 0xD0.toByte(), 0x42),
            encoded,
        )
    }

    @Test
    fun big5EncodesTraditionalChinese() {
        val encoded = CharsetEncoder.encode("中", PrinterCharset.BIG5)
        assertEquals(2, encoded.size, "BIG5 uses two bytes per CJK character")
        assertContentEquals(listOf(0xA4.toByte(), 0xA4.toByte()), encoded.toList())
    }

    @Test
    fun unsupportedCharactersBecomeQuestionMark() {
        // Emoji are outside GBK, BIG5, CP437 and both Latin variants.
        val emoji = "\uD83D\uDE00"
        for (charset in listOf(
            PrinterCharset.GBK,
            PrinterCharset.BIG5,
            PrinterCharset.CP437,
            PrinterCharset.ISO8859_1,
            PrinterCharset.WINDOWS_1252,
        )) {
            assertContentEquals(
                listOf(0x3F.toByte(), 0x3F.toByte()),
                CharsetEncoder.encode(emoji, charset).toList(),
                "unmapped surrogate must degrade in ${charset.value}",
            )
        }
    }

    @Test
    fun unknownCharsetFallsBackToUtf8RatherThanLosingText() {
        // Better a readable UTF-8 receipt than a page of '?' characters.
        // Expected bytes are spelled out so the test does not depend on
        // String.encodeToByteArray, which is missing on some targets.
        val expected = listOf(
            0xE3.toByte(), 0x81.toByte(), 0x93.toByte(), // こ
            0xE3.toByte(), 0x82.toByte(), 0x93.toByte(), // ん
            0xE3.toByte(), 0x81.toByte(), 0xAB.toByte(), // に
            0xE3.toByte(), 0x81.toByte(), 0xA1.toByte(), // ち
            0xE3.toByte(), 0x81.toByte(), 0xAF.toByte(), // は
        )
        assertContentEquals(
            expected,
            CharsetEncoder.encode("こんにちは", "NOT-A-CHARSET").toList(),
        )
    }

    @Test
    fun charsetAliasesAreAccepted() {
        val variants = listOf("gbk", "GBK", "  gbk  ", "GB2312", "CP936", "GB18030")
        val expected = CharsetEncoder.encode("中", PrinterCharset.GBK).toList()
        for (alias in variants) {
            assertContentEquals(expected, CharsetEncoder.encode("中", alias).toList(), "alias $alias")
        }
    }

    @Test
    fun doubleByteOutputHasNoTrailingPadding() {
        // "中A" needs 3 bytes, not the 4 the worst-case buffer allows.
        val encoded = CharsetEncoder.encode("中A", PrinterCharset.GBK)
        assertEquals(3, encoded.size)
        assertTrue(encoded[2] == 0x41.toByte(), "trailing byte must be 'A'")
    }

    @Test
    fun latinAccentsRenderInEveryLatinCapableCharset() {
        for (charset in listOf(PrinterCharset.ISO8859_1, PrinterCharset.WINDOWS_1252)) {
            val encoded = CharsetEncoder.encode("Café", charset)
            assertEquals(4, encoded.size, "no replacement expected in ${charset.value}")
            assertContentEquals(listOf(0xE9.toByte()), encoded.toList().subList(3, 4))
        }
    }
}