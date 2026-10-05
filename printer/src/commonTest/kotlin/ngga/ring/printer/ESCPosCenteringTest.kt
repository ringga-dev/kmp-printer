package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.ESCPosConfig
import ngga.ring.printer.util.escpos.ESCPosTextLayout
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.PreviewBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ESCPosCenteringTest {

    @Test
    fun wrapTextEmpty() {
        assertEquals(listOf(""), ESCPosTextLayout.wrapText("", 31))
    }

    @Test
    fun wrapTextBreaksAtWordBoundary() {
        val lines = ESCPosTextLayout.wrapText("Jl. Contoh Alamat No. 123, Kota, Provinsi", 20)
        assertTrue(lines.isNotEmpty())
        lines.forEach { assertTrue(it.length <= 20, "line too long: $it") }
        // No word lost: joining with space equals normalized original
        assertEquals("Jl. Contoh Alamat No. 123, Kota, Provinsi", lines.joinToString(" "))
    }

    @Test
    fun wrapTextHardSplitsLongWord() {
        assertEquals(
            listOf("AAAAAAAAAA", "AAAAAAAAAA", "AAAAA"),
            ESCPosTextLayout.wrapText("AAAAAAAAAAAAAAAAAAAAAAAAA", 10)
        )
    }

    @Test
    fun centeredTextBalancedAndFullWidth() {
        val result = ESCPosTextLayout.centeredText("abc", 32)
        val safeMax = 31
        assertEquals(safeMax, result.length)
        val trimmed = result.trim(' ')
        assertEquals("abc", trimmed)
        val leftPad = result.indexOf('a')
        val rightPad = result.length - result.lastIndexOf('c') - 1
        assertTrue(kotlin.math.abs(leftPad - rightPad) <= 1, "leftPad=$leftPad rightPad=$rightPad")
    }

    @Test
    fun centeredTextNearFullWidthKeepsContent() {
        // A text that fills the width must be preserved (not truncated/lost).
        val full = "x".repeat(31)
        val result = ESCPosTextLayout.centeredText(full, 32)
        assertTrue(result.contains(full))
    }

    @Test
    fun centerTextProducesHardwareAlignmentBytes() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.centerText("abc")
        val bytes = builder.build().toList()
        val textBytes = "abc".encodeToByteArray().toList()
        // Must contain ESC a 1, text, LF, then ESC a 0
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) })
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte()) })
        assertTrue(bytes.windowed(4).any { it == textBytes + listOf(0x0A.toByte()) })
    }

    @Test
    fun centerTextPreviewBlockHasCenterAlignment() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.centerText("abc")
        val texts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertEquals(1, texts.size)
        assertEquals(TextAlignment.CENTER, texts.first().alignment)
    }

    @Test
    fun centerWrappedUsesHardwareAlignment() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.centerWrapped("Jl. Contoh Alamat No. 123, Kota, Provinsi", maxLine = 5)
        val bytes = builder.build().toList()
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) })
        val texts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertTrue(texts.isNotEmpty())
        texts.forEach { assertEquals(TextAlignment.CENTER, it.alignment) }
    }

    @Test
    fun centerTextMultiLineStaysCentered() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        // Long text that wraps into more than 2 lines
        builder.centerText("Jl. Contoh Alamat No. 123, Kelurahan Damai, Kecamatan Sejahtera, Kota Bahagia, Provinsi Makmur")
        val texts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertTrue(texts.size > 2, "expected >2 lines, got ${texts.size}")
        texts.forEach { assertEquals(TextAlignment.CENTER, it.alignment) }

        // Every text line must appear AFTER the single ESC a 1 and BEFORE ESC a 0
        val bytes = builder.build().toList()
        val enterCenter = bytes.indexOf(0x1B.toByte()) // first ESC is 'a' command below
        fun indexOfSeq(vararg target: Byte): Int {
            for (i in 0..bytes.size - target.size) {
                if (target.withIndex().all { (j, b) -> bytes[i + j] == b }) return i
            }
            return -1
        }
        val enterIdx = indexOfSeq(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
        val exitIdx = indexOfSeq(0x1B.toByte(), 0x61.toByte(), 0x00.toByte())
        assertTrue(enterIdx >= 0 && exitIdx > enterIdx, "hardware center not bracketed: enter=$enterIdx exit=$exitIdx")
        val firstTextIdx = bytes.indexOf('J'.code.toByte())
        val lastLf = bytes.lastIndexOf(0x0A.toByte())
        assertTrue(firstTextIdx > enterIdx && lastLf < exitIdx, "text lines not inside centered region")
    }

    @Test
    fun centerWrappedMultiLineStaysCentered() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.centerWrapped("Jl. Contoh Alamat No. 123, Kelurahan Damai, Kecamatan Sejahtera, Kota Bahagia", maxLine = 5)
        val texts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertTrue(texts.size > 2, "expected >2 lines, got ${texts.size}")
        texts.forEach { assertEquals(TextAlignment.CENTER, it.alignment) }
    }

    @Test
    fun lineSpacingFromConfigEmitsEsc3() {
        val builder = ESCPosCommandBuilder(
            ESCPosConfig(charsPerLine = 32, paperWidthDots = 384, lineSpacing = 40)
        ).initialize()
        val bytes = builder.build().toList()
        val idx = bytes.windowed(3).indexOfFirst { it == listOf(0x1B.toByte(), 0x33.toByte(), 40.toByte()) }
        assertTrue(idx >= 0, "expected ESC 3 40 (set line spacing) in bytes")
    }

    @Test
    fun qrCodeCenterStillWorks() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.qrCode("https://example.com", center = true)
        val bytes = builder.build().toList()
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) })
    }

    @Test
    fun barcodeCenterStillWorks() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.barcode("123456", center = true)
        val bytes = builder.build().toList()
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) })
    }

    @Test
    fun imageCenterStillWorks() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32, paperWidthDots = 384))
        builder.image(ByteArray(16), 8, 8, center = true)
        val bytes = builder.build().toList()
        assertTrue(bytes.windowed(3).any { it == listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) })
    }
}
