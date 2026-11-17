package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.ESCPosConfig
import ngga.ring.printer.util.escpos.ESCPosTextLayout
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.PreviewBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ESCPosTableLayoutTest {

    @Test
    fun testTableRowDefaultAlignments() {
        // maxCharsPerLine = 31 -> safeMax = 30
        // weights [1, 1, 1] -> widths [10, 10, 10]
        // Default alignments: col 0 = LEFT, col 1 = CENTER, col 2 = RIGHT
        val row = ESCPosTextLayout.tableRow(
            columns = listOf("Item", "1", "100"),
            weights = listOf(1, 1, 1),
            maxCharsPerLine = 31
        )
        assertEquals(30, row.length)
        assertEquals("Item          1            100", row)
    }

    @Test
    fun testTableRowCustomAlignments() {
        // maxCharsPerLine = 31 -> safeMax = 30
        // weights [1, 1, 1] -> widths [10, 10, 10]
        // Custom alignments: [LEFT, LEFT, RIGHT]
        val row = ESCPosTextLayout.tableRow(
            columns = listOf("Item", "Center", "Price"),
            weights = listOf(1, 1, 1),
            maxCharsPerLine = 31,
            alignments = listOf(TextAlignment.LEFT, TextAlignment.LEFT, TextAlignment.RIGHT)
        )
        assertEquals(30, row.length)
        assertEquals("Item      Center         Price", row)
    }

    @Test
    fun testTableRowWrappedMultiline() {
        // maxCharsPerLine = 31 -> safeMax = 30
        // weights [1, 1, 1] -> widths [10, 10, 10]
        // Col 0 will wrap into 3 lines of max width 10: "A very", "long item", "name here"
        val lines = ESCPosTextLayout.tableRowWrapped(
            columns = listOf("A very long item name here", "2", "50000"),
            weights = listOf(1, 1, 1),
            maxCharsPerLine = 31,
            alignments = listOf(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT)
        )

        assertEquals(3, lines.size)
        // All lines must adhere to the 30 chars safe line width
        lines.forEach { assertEquals(30, it.length) }

        assertEquals("A very        2          50000", lines[0])
        assertEquals("long item                     ", lines[1])
        assertEquals("name here                     ", lines[2])
    }

    @Test
    fun testCommandBuilderTableRowAutoWrap() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 31))
        builder.tableRow(
            columns = listOf("A very long item name here", "2", "50000"),
            weights = listOf(1, 1, 1),
            alignments = listOf(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT),
            autoWrap = true
        )

        val previewTexts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertEquals(3, previewTexts.size)
        assertEquals("A very        2          50000", previewTexts[0].text)
        assertEquals("long item                     ", previewTexts[1].text)
        assertEquals("name here                     ", previewTexts[2].text)
    }

    @Test
    fun testCommandBuilderTableRowWrappedHelper() {
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 31))
        builder.tableRowWrapped(
            columns = listOf("A very long item name here", "2", "50000"),
            weights = listOf(1, 1, 1),
            alignments = listOf(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT)
        )

        val previewTexts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertEquals(3, previewTexts.size)
        assertEquals("A very        2          50000", previewTexts[0].text)
    }

    @Test
    fun testCommandLineWrappedWithoutTruncation() {
        // charsPerLine = 32 -> safeMax = 31
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32))
        val sentence = "The quick brown fox jumps over the lazy dog and runs away fast"
        assertEquals(62, sentence.length)

        builder.lineWrapped(sentence)

        val previewTexts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertEquals(2, previewTexts.size)

        // Verify wrapped lines reconstruct the original sentence without truncation
        val reconstructed = "${previewTexts[0].text} ${previewTexts[1].text}"
        assertEquals(sentence, reconstructed)

        // Verify LF bytes (0x0A) were output for each wrapped line
        val bytes = builder.build()
        val lfCount = bytes.count { it == 0x0A.toByte() }
        assertEquals(2, lfCount)
    }
}
