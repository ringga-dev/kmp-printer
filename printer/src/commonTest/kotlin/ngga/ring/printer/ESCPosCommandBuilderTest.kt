package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.model.QRCodeLevel
import ngga.ring.printer.model.BarcodeType
import ngga.ring.printer.model.PrintQuality
import kotlin.test.Test
import kotlin.test.assertTrue

class ESCPosCommandBuilderTest {

    @Test
    fun testInitialize() {
        val builder = ESCPosCommandBuilder().initialize()
        val bytes = builder.build()
        // ESC @ is 1B 40
        assertTrue(bytes.size >= 2)
        assertTrue(bytes[0] == 0x1B.toByte())
        assertTrue(bytes[1] == 0x40.toByte())
    }

    @Test
    fun testBeep() {
        val builder = ESCPosCommandBuilder().beep(3, 2)
        val bytes = builder.build()
        // ESC ( A 02 00 03 02 -> 1B 28 41 02 00 03 02
        val expected = byteArrayOf(0x1B, 0x28, 0x41, 0x02, 0x00, 0x03, 0x02)
        assertTrue(bytes.contentEquals(expected))
    }

    @Test
    fun testQRCodeLevel() {
        val builder = ESCPosCommandBuilder().qrCode("test", level = QRCodeLevel.H)
        val bytes = builder.build()
        // Find the error correction level byte (51 for Level H)
        // 1D 28 6B 03 00 31 45 n
        var found = false
        for (i in 0 until bytes.size - 7) {
            if (bytes[i] == 0x1D.toByte() && bytes[i+1] == 0x28.toByte() && 
                bytes[i+2] == 0x6B.toByte() && bytes[i+5] == 0x31.toByte() && 
                bytes[i+6] == 0x45.toByte()) {
                if (bytes[i+7] == 51.toByte()) {
                    found = true
                    break
                }
            }
        }
        assertTrue(found, "QR Code Level H byte (51) not found in expected sequence")
    }

    @Test
    fun testPrintDensity() {
        val bytes = ESCPosCommandBuilder().setPrintDensity(99).build()
        val expected = byteArrayOf(0x12, 0x23, 0x0F)
        assertTrue(bytes.contentEquals(expected))
    }

    @Test
    fun testHeatConfig() {
        val bytes = ESCPosCommandBuilder().setHeatConfig(dots = 11, time = 120, interval = 40).build()
        val expected = byteArrayOf(0x1B, 0x37, 0x0B, 0x78, 0x28)
        assertTrue(bytes.contentEquals(expected))
    }

    @Test
    fun testPrintQualityDark() {
        val bytes = ESCPosCommandBuilder().printQuality(PrintQuality.Dark).build()
        val expected = byteArrayOf(0x12, 0x23, 0x0F, 0x1B, 0x37, 0x0B, 0x78, 0x28)
        assertTrue(bytes.contentEquals(expected))
    }
}
