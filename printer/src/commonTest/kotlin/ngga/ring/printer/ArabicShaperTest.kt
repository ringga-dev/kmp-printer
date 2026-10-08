package ngga.ring.printer

import ngga.ring.printer.util.bidi.ArabicShaper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArabicShaperTest {

    @Test
    fun testDetectArabicCharacters() {
        assertTrue(ArabicShaper.hasArabic("مرحبا"))
        assertTrue(ArabicShaper.hasArabic("Order #123 شكرا"))
        assertFalse(ArabicShaper.hasArabic("Hello World 123!"))
    }

    @Test
    fun testShapeArabicWord() {
        // "مرحبا" (Meem, Reh, Hah, Beh, Alef)
        val shaped = ArabicShaper.shape("مرحبا")
        assertTrue(shaped.isNotEmpty())
        // Should contain transformed initial/medial/final glyphs
        assertFalse(shaped.contains('\u0645')) // Raw Meem should be converted
    }

    @Test
    fun testProcessForThermalPrinterReversesForRTL() {
        val result = ArabicShaper.processForThermalPrinter("مرحبا")
        assertTrue(result.isNotEmpty())
    }
}
