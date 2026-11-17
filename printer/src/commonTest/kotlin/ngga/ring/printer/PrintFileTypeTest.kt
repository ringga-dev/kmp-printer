package ngga.ring.printer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ngga.ring.printer.model.PrintFileType

class PrintFileTypeTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun `detects PNG from its 8 byte signature`() {
        val png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals(PrintFileType.PNG, PrintFileType.fromBytes(png))
    }

    @Test
    fun `detects JPEG from the SOI marker`() {
        val jpeg = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10)
        assertEquals(PrintFileType.JPEG, PrintFileType.fromBytes(jpeg))
    }

    @Test
    fun `detects GIF from its header`() {
        val gif = bytes(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) // GIF89a
        assertEquals(PrintFileType.GIF, PrintFileType.fromBytes(gif))
    }

    @Test
    fun `detects BMP from its header`() {
        val bmp = bytes(0x42, 0x4D, 0x46, 0x00, 0x00, 0x00)
        assertEquals(PrintFileType.BMP, PrintFileType.fromBytes(bmp))
    }

    @Test
    fun `detects PDF from the percent-PDF header`() {
        val pdf = bytes(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37)
        assertEquals(PrintFileType.PDF, PrintFileType.fromBytes(pdf))
    }

    @Test
    fun `detects WebP from the RIFF container`() {
        // RIFF + 4 length bytes + WEBP
        val webp = bytes(
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50
        )
        assertEquals(PrintFileType.WEBP, PrintFileType.fromBytes(webp))
    }

    @Test
    fun `WebP needs 12 bytes so short buffers do not match`() {
        // RIFF present but truncated before the WEBP tag
        val truncated = bytes(0x52, 0x49, 0x46, 0x46, 0x00, 0x00)
        assertNull(PrintFileType.fromBytes(truncated))
    }

    @Test
    fun `returns null for unknown bytes`() {
        assertNull(PrintFileType.fromBytes(bytes(0x00, 0x01, 0x02, 0x03)))
        assertNull(PrintFileType.fromBytes(bytes(0x7B, 0x22, 0x6A, 0x73, 0x6F, 0x6E, 0x22, 0x3A))) // {"json":
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(PrintFileType.fromBytes(ByteArray(0)))
    }

    @Test
    fun `returns null when the buffer is shorter than the signature`() {
        // Only 2 bytes of the PNG signature
        assertNull(PrintFileType.fromBytes(bytes(0x89, 0x50)))
    }

    @Test
    fun `PNG signature must match in full`() {
        // Starts with the PNG magic but the fourth byte differs
        val fake = bytes(0x89, 0x50, 0x4E, 0x58, 0x0D, 0x0A, 0x1A, 0x0A)
        assertNull(PrintFileType.fromBytes(fake))
    }
}
