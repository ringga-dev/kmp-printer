package ngga.ring.printer

import ngga.ring.printer.util.tspl.TsplCommandBuilder
import kotlin.test.Test
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
        assertTrue(text.contains("SIZE 40.0 mm, 30.0 mm"))
        assertTrue(text.contains("CLS"))
        assertTrue(text.contains("TEXT 10,10,\"3\",0,1,1,\"PRINTER TEST\""))
        assertTrue(text.contains("BARCODE 10,40,\"128\",40,2,0,2,4,\"1234567890\""))
        assertTrue(text.contains("QRCODE 200,40,M,4,A,0,\"https://github.com/ringga-dev/kmp-printer\""))
        assertTrue(text.contains("PRINT 1,1"))
    }
}
