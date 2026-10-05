package ngga.ring.printer

import ngga.ring.printer.util.escpos.ESCPosImageHelper
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the shared raster packing and dithering behaviour.
 *
 * These run on every platform, so a change here affects Android, JVM, iOS and JS
 * output identically. That is the point: the per-platform image helpers must all
 * produce the same bytes for the same pixels.
 */
class ESCPosRasterTest {

    @Test
    fun packsEightPixelsPerByteMostSignificantFirst() {
        // Row of alternating pixels starting black: 10101010 -> 0xAA
        val pixels = BooleanArray(8) { it % 2 == 0 }
        val bytes = ESCPosImageHelper.packPixelsToRaster(pixels, 8, 1)

        assertEquals(1, bytes.size)
        assertContentEquals(listOf(0xAA.toByte()), bytes.toList())
    }

    @Test
    fun padsPartialBytesWithZeroes() {
        // Three black pixels fill 0xE0, the remaining five bits stay unset.
        val pixels = BooleanArray(3) { true }
        val bytes = ESCPosImageHelper.packPixelsToRaster(pixels, 3, 1)

        assertEquals(1, bytes.size)
        assertContentEquals(listOf(0xE0.toByte()), bytes.toList())
    }

    @Test
    fun rowsArePackedIndependently() {
        val width = 8
        val pixels = BooleanArray(width * 2) { index ->
            index % width == 7 // only the last pixel of each row is black
        }
        val bytes = ESCPosImageHelper.packPixelsToRaster(pixels, width, 2)

        assertEquals(2, bytes.size)
        assertContentEquals(listOf(0x01.toByte(), 0x01.toByte()), bytes.toList())
    }

    @Test
    fun pureBlackBecomesAllSetBits() {
        val pixels = BooleanArray(16) { true }
        val bytes = ESCPosImageHelper.packPixelsToRaster(pixels, 16, 1)

        assertContentEquals(listOf(0xFF.toByte(), 0xFF.toByte()), bytes.toList())
    }

    @Test
    fun pureWhiteProducesZeroedRaster() {
        val pixels = BooleanArray(16) { false }
        val bytes = ESCPosImageHelper.packPixelsToRaster(pixels, 16, 1)

        assertContentEquals(listOf(0x00.toByte(), 0x00.toByte()), bytes.toList())
    }

    @Test
    fun ditheringSplitsUniformMidGray() {
        // 128 is the threshold boundary, so a uniform field must come out as a
        // mix rather than a solid block.
        val width = 8
        val height = 8
        val gray = IntArray(width * height) { 128 }
        val result = ESCPosImageHelper.applyFloydSteinberg(gray, width, height)

        val black = result.count { it }
        assertTrue(black > 0, "dithering must set some pixels")
        assertTrue(black < result.size, "dithering must leave some pixels white")
    }

    @Test
    fun ditheringMapsBlackToBlackAndWhiteToWhite() {
        val width = 4
        val height = 4

        val allBlack = IntArray(width * height) { 0 }
        assertTrue(
            ESCPosImageHelper.applyFloydSteinberg(allBlack, width, height).all { it },
            "zero intensity must dither to solid black",
        )

        val allWhite = IntArray(width * height) { 255 }
        assertTrue(
            ESCPosImageHelper.applyFloydSteinberg(allWhite, width, height).none { it },
            "full intensity must dither to solid white",
        )
    }

    @Test
    fun ditheringIsMonotonicAcrossAGrayGradient() {
        // A left-to-right ramp: the dark quarter of each row must produce more
        // black pixels than the light quarter.
        val width = 32
        val height = 16
        val gray = IntArray(width * height) { index ->
            (index % width) * (255 / (width - 1))
        }
        val result = ESCPosImageHelper.applyFloydSteinberg(gray, width, height)

        val darkCount = (0 until height).sumOf { y ->
            (0 until width / 4).count { x -> result[y * width + x] }
        }
        val lightCount = (0 until height).sumOf { y ->
            (width - width / 4 until width).count { x -> result[y * width + x] }
        }

        assertTrue(
            darkCount > lightCount,
            "dark quarter must dither darker than light quarter " +
                "(dark=$darkCount, light=$lightCount)",
        )
    }

    @Test
    fun atkinsonDitheringIsAlsoAvailableAndBinary() {
        val width = 8
        val height = 8
        val gray = IntArray(width * height) { 128 }
        val result = ESCPosImageHelper.applyAtkinson(gray, width, height)

        assertEquals(width * height, result.size)
        val black = result.count { it }
        assertTrue(black in 1 until result.size)
    }

    @Test
    fun contrastAndBrightnessStayWithinByteRange() {
        val gray = IntArray(256) { it }

        val brightened = ESCPosImageHelper.adjustLevels(gray, contrast = 0, brightness = 100)
        assertTrue(brightened.all { it in 0..255 })

        val darkened = ESCPosImageHelper.adjustLevels(gray, contrast = 0, brightness = -100)
        assertTrue(darkened.all { it in 0..255 })

        // A brightness bump can only raise values, never lower them.
        assertTrue(brightened[200] > gray[200])
        assertTrue(darkened[50] < gray[50])
    }

    @Test
    fun grayscaleRasterPacksBelowThresholdAsBlack() {
        val pixels = IntArray(8) { if (it < 4) 0 else 255 }
        val bytes = ESCPosImageHelper.packGrayscaleToRaster(pixels, 8, 1)

        // First four pixels set, remaining nibble unset.
        assertContentEquals(listOf(0xF0.toByte()), bytes.toList())
    }
}