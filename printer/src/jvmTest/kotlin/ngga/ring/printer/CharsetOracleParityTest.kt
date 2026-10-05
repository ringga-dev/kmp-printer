package ngga.ring.printer

import ngga.ring.printer.model.PrinterCharset
import ngga.ring.printer.util.platform.CharsetEncoder
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Verifies the shared pure Kotlin tables against Java's `Charset`, which is what
 * the JVM target uses.
 *
 * The tables were generated from these very tables, so this test guards against
 * regeneration mistakes and against drift if either side changes.
 *
 * Runs on JVM only: it needs `java.nio.charset` as the reference.
 */
class CharsetOracleParityTest {

    @Test
    fun singleByteCharsetsMatchJava() {
        val samples = buildList {
            addAll(ASCII_SAMPLES)
            addAll(LATIN_SAMPLES)
            addAll(BOX_SAMPLES)
        }
        for ((charsetEnum, javaName) in singleByteCharsets()) {
            val java = Charset.forName(javaName)
            for (ch in samples) {
                val expected = ch.toString().toByteArray(java).toList()
                val actual = CharsetEncoder.encode(ch.toString(), charsetEnum).toList()
                assertContentEquals(
                    expected,
                    actual,
                    "${charsetEnum.value}: sample U+%04X".format(ch.code),
                )
            }
        }
    }

    @Test
    fun gbkMatchesJavaForMappedCharacters() {
        val java = Charset.forName("GBK")
        for (ch in gbkSamples()) {
            val expected = ch.toString().toByteArray(java)
            // Java emits '?' for unmappable characters too, so a mismatch only
            // matters when one side mapped it and the other did not.
            val actual = CharsetEncoder.encode(ch.toString(), PrinterCharset.GBK)
            assertContentEquals(
                expected.toList(),
                actual.toList(),
                "GBK mismatch for U+%04X".format(ch.code),
            )
        }
    }

    @Test
    fun big5MatchesJavaForMappedCharacters() {
        val java = Charset.forName("BIG5")
        for (ch in big5Samples()) {
            val expected = ch.toString().toByteArray(java)
            val actual = CharsetEncoder.encode(ch.toString(), PrinterCharset.BIG5)
            assertContentEquals(
                expected.toList(),
                actual.toList(),
                "BIG5 mismatch for U+%04X".format(ch.code),
            )
        }
    }

    @Test
    fun everyMappedPositionRoundTripsThroughItsOwnTable() {
        // For every character the generated tables do map, encoding must not
        // degrade to '?'. The sample set is filtered through Java first so that
        // codepoints unassigned in the standard (such as U+9FA6, which sits in
        // the CJK block but has no GBK encoding) are not counted as regressions.
        for ((charsetEnum, javaName) in doubleByteCharsets()) {
            val java = Charset.forName(javaName)
            val mapped = charsetSamples(charsetEnum).filter { ch ->
                ch.toString().toByteArray(java).none { it == REPLACEMENT }
            }
            assertTrue(mapped.isNotEmpty(), "no mapped samples for ${charsetEnum.value}")

            for (ch in mapped) {
                val encoded = CharsetEncoder.encode(ch.toString(), charsetEnum)
                assertTrue(
                    encoded.isNotEmpty(),
                    "%s: U+%04X produced no output".format(charsetEnum.value, ch.code),
                )
                assertTrue(
                    encoded.none { it == REPLACEMENT },
                    "%s: U+%04X is mapped by Java but degraded to '?'".format(charsetEnum.value, ch.code),
                )
            }
        }
    }

    /* ----------------------------------------------------------------- fixtures */

    private fun singleByteCharsets(): List<Pair<PrinterCharset, String>> = listOf(
        PrinterCharset.CP437 to "IBM437",
        PrinterCharset.ISO8859_1 to "ISO-8859-1",
        PrinterCharset.WINDOWS_1252 to "windows-1252",
    )

    private fun doubleByteCharsets(): List<Pair<PrinterCharset, String>> = listOf(
        PrinterCharset.GBK to "GBK",
        PrinterCharset.BIG5 to "Big5",
    )

    private fun charsetSamples(charset: PrinterCharset): List<Char> =
        when (charset) {
            PrinterCharset.GBK -> gbkSamples()
            PrinterCharset.BIG5 -> big5Samples()
            else -> emptyList()
        }

    private fun gbkSamples(): List<Char> =
        buildList {
            // Common simplified Chinese, then a spread across the lead range.
            for (ch in "的一是不了人我在有他这为之大来以个中上们到说国和地也子时道出而要于就下得可你年生") add(ch)
            for (ch in "あいうえおカタカナ漢字全角ＡＢＣ１２３") add(ch)
            for (cp in 0x4E00..0x4E40) add(cp.toChar())
            for (cp in 0x9FA6..0x9FC0) add(cp.toChar())
            for (cp in 0x3400..0x3420) add(cp.toChar())
        }

    private fun big5Samples(): List<Char> =
        buildList {
            for (ch in "的一是不了人我在有他這之大來以個中上們到說國和地也子時道出而要於就下得可你年生") add(ch)
            for (ch in "あいうえおカタカナ漢字全角ＡＢＣ１２３") add(ch)
            for (cp in 0x4E00..0x4E40) add(cp.toChar())
            for (cp in 0x9FA6..0x9FC0) add(cp.toChar())
            for (cp in 0xF900..0xF920) add(cp.toChar())
        }

    private companion object {
        const val REPLACEMENT: Byte = 0x3F

        val ASCII_SAMPLES = (' '..'~').toList()
        val LATIN_SAMPLES = listOf(
            'à', 'é', 'ü', 'ñ', 'ç', 'ß', 'å', 'ø', 'æ', 'ÿ',
            'À', 'É', 'Ü', 'Ñ', 'Ç', 'Å', 'Ø', 'Æ',
        )
        val BOX_SAMPLES = listOf(
            '░', '▒', '▓', '│', '┤', '╡', '╢', '╖', '╕', '╣', '║', '╗', '╝', '╜', '╛', '┐',
            '└', '┴', '┬', '├', '─', '┼', '╞', '╟', '╚', '╔', '╩', '╦', '╠', '═', '╬', '╧',
            '╨', '╤', '╥', '╙', '╘', '╒', '╓', '╫', '╪', '┘', '┌', '█', '▄', '▌', '▐', '▀',
        )
    }
}