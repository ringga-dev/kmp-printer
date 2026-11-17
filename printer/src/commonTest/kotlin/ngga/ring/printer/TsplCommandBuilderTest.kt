package ngga.ring.printer

import ngga.ring.printer.util.tspl.TsplCommandBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TsplCommandBuilderTest {

    @Test
    fun testBuildBasicLabel() {
        val bytes = TsplCommandBuilder(widthMm = 40.0, heightMm = 30.0)
            .direction(0)
            .text(x = 10, y = 10, content = "PRINTER TEST", font = "3")
            .barcode(x = 10, y = 40, data = "1234567890", type = "128", height = 40)
            .qrcode(x = 200, y = 40, data = "https://github.com/ringga-dev/kmp-printer")
            .print(1, 1)
            .build()

        val text = bytes.decodeToString()
        assertTrue(text.contains("SIZE ${40.0} mm, ${30.0} mm"))
        assertTrue(text.contains("CLS"))
        assertTrue(text.contains("TEXT 10,10,\"3\",0,1,1,\"PRINTER TEST\""))
        assertTrue(text.contains("BARCODE 10,40,\"128\",40,2,0,2,4,\"1234567890\""))
        assertTrue(text.contains("QRCODE 200,40,M,4,A,0,\"https://github.com/ringga-dev/kmp-printer\""))
        assertTrue(text.contains("PRINT 1,1"))
    }

    @Test
    fun testBitmapCommandOrder() {
        val bitmapData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val bytes = TsplCommandBuilder(widthMm = 40.0, heightMm = 30.0)
            .cls()
            .bitmap(0, 0, 2, 2, 0, bitmapData)
            .print(1, 1)
            .build()

        val header = "BITMAP 0,0,2,2,0,".encodeToByteArray()
        val crlf = "\r\n".encodeToByteArray()

        fun indexOfSequence(source: ByteArray, target: ByteArray): Int {
            if (target.isEmpty()) return 0
            outer@ for (i in 0..source.size - target.size) {
                for (j in target.indices) {
                    if (source[i + j] != target[j]) continue@outer
                }
                return i
            }
            return -1
        }

        val headerIndex = indexOfSequence(bytes, header)
        assertTrue(headerIndex >= 0, "Expected BITMAP header not found in output bytes")

        val payloadStart = headerIndex + header.size
        for (i in bitmapData.indices) {
            assertEquals(bitmapData[i], bytes[payloadStart + i], "Byte at offset $i mismatch")
        }

        val crlfStart = payloadStart + bitmapData.size
        assertEquals(crlf[0], bytes[crlfStart], "CR byte mismatch")
        assertEquals(crlf[1], bytes[crlfStart + 1], "LF byte mismatch")
    }

    @Test
    fun testAutoAppendPrintCommand() {
        val bytes = TsplCommandBuilder(widthMm = 40.0, heightMm = 30.0)
            .cls()
            .text(10, 10, "NO PRINT EXPLICIT")
            .build()

        val text = bytes.decodeToString()
        assertTrue(text.endsWith("PRINT 1,1\r\n"))
    }

    @Test
    fun testExplicitPrintCommandNotOverridden() {
        val bytes = TsplCommandBuilder(widthMm = 40.0, heightMm = 30.0)
            .cls()
            .text(10, 10, "EXPLICIT PRINT")
            .print(3, 2)
            .build()

        val text = bytes.decodeToString()
        assertTrue(text.contains("PRINT 3,2\r\n"))
        assertFalse(text.contains("PRINT 1,1"))
    }
}
