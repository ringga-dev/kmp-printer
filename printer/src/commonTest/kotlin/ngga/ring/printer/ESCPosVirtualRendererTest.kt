package ngga.ring.printer

import ngga.ring.printer.util.preview.ESCPosVirtualRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class ESCPosVirtualRendererTest {

    @Test
    fun testDecodeExtendedAsciiCP437() {
        // ESC t 0 (CP437), then bytes for "Señor" (0x53, 0x65, 0xA4, 0x6F, 0x72), LF
        val data = byteArrayOf(
            0x1B, 0x74, 0x00, // ESC t 0
            0x53, 0x65, 0xA4.toByte(), 0x6F, 0x72, // "Señor"
            0x0A
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertEquals(1, lines.size)
        assertEquals("Señor", lines[0].content)
    }

    @Test
    fun testDecodeExtendedAsciiBoxCharacters() {
        // Default code page is CP437. Box drawing: 0xC4 = '─', 0xB0 = '░'
        val data = byteArrayOf(
            0xC4.toByte(), 0xB0.toByte(),
            0x0A
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertEquals(1, lines.size)
        assertEquals("─░", lines[0].content)
    }

    @Test
    fun testDecodeExtendedAsciiWindows1252() {
        // ESC t 16 (0x10), then 0x80 (Euro symbol '€' in Windows-1252)
        val data = byteArrayOf(
            0x1B, 0x74, 0x10,
            0x80.toByte(),
            0x0A
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertEquals(1, lines.size)
        assertEquals("€", lines[0].content)
    }

    @Test
    fun testDecodeExtendedAsciiIso8859_1() {
        // ESC t 1 (ISO-8859-1), then 0xA4 (Currency sign '¤')
        val data = byteArrayOf(
            0x1B, 0x74, 0x01,
            0xA4.toByte(),
            0x0A
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertEquals(1, lines.size)
        assertEquals("¤", lines[0].content)
    }

    @Test
    fun testEscAtResetsCodePageToCP437() {
        // Set to Windows-1252, then ESC @ (reset), then 0x80 (should be 'Ç' in CP437, not '€')
        val data = byteArrayOf(
            0x1B, 0x74, 0x10, // Windows-1252
            0x1B, 0x40,       // Reset (ESC @)
            0x80.toByte(),
            0x0A
        )
        val lines = ESCPosVirtualRenderer.render(data)
        assertEquals(1, lines.size)
        assertEquals("Ç", lines[0].content)
    }
}
