package ngga.ring.printer.receipt

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage

/**
 * [PdfToEscPos] converts PDF content into ESC/POS raster commands,
 * enabling thermal printers to print PDF invoices, receipts, etc.
 *
 * Usage:
 * ```kotlin
 * val converter = PdfToEscPos()
 * val pdfBytes = File("invoice.pdf").readBytes()
 * val escPosBytes = converter.convert(pdfBytes, PaperWidth.MM_80)
 * printer.printRaw(config, escPosBytes)
 * ```
 *
 * The conversion process:
 * 1. Load PDF with Apache PDFBox
 * 2. Render each page to a 203-DPI bitmap
 * 3. Dither to monochrome (1-bit) for thermal printers
 * 4. Wrap monochrome raster in ESC/POS GS 'v' '0' raster commands
 */
public class PdfToEscPos {

    /** Thermal paper width options. */
    public enum class PaperWidth(val mm: Int, val dots: Int) {
        MM_58(58, 384),   // 58mm = 384 dots at 203 DPI
        MM_80(80, 576),   // 80mm = 576 dots at 203 DPI
        MM_112(112, 832); // 112mm = 832 dots at 203 DPI
    }

    /**
     * Convert PDF bytes to ESC/POS raster commands.
     *
     * @param pdfBytes Raw PDF file content.
     * @param paperWidth Target thermal paper width.
     * @param dpi Rendering resolution (default: 203 DPI — standard for thermal).
     * @return ESC/POS byte array ready to send to the printer.
     */
    public fun convert(
        pdfBytes: ByteArray,
        paperWidth: PaperWidth = PaperWidth.MM_80,
        dpi: Int = 203
    ): ByteArray {
        val document = Loader.loadPDF(pdfBytes)
        val renderer = PDFRenderer(document)
        val output = ByteArrayOutputStream()

        try {
            for (page in 0 until document.numberOfPages) {
                // Render page to RGB bitmap at target DPI
                val pageImage = renderer.renderImageWithDPI(page, dpi.toFloat())

                // Scale/crop to fit the paper width
                val resized = fitToPaper(pageImage, paperWidth)

                // Dither to 1-bit monochrome
                val bwImage = ditherFloydSteinberg(resized)

                // Encapsulate in ESC/POS raster command
                val rasterBytes = buildRasterCommand(bwImage)
                output.write(rasterBytes)

                // Line feed between pages
                output.write(byteArrayOf(0x0A, 0x0A))
            }
        } finally {
            document.close()
        }

        return output.toByteArray()
    }

    /**
     * Convert a single [BufferedImage] directly to ESC/POS raster.
     * Useful for printing non-PDF images or composing custom layouts.
     */
    public fun convertImage(
        image: BufferedImage,
        paperWidth: PaperWidth = PaperWidth.MM_80
    ): ByteArray {
        val resized = fitToPaper(image, paperWidth)
        val bwImage = ditherFloydSteinberg(resized)
        return buildRasterCommand(bwImage)
    }

    // ─── Private ─────────────────────────────────────

    /**
     * Resize the image to fit the paper width while preserving aspect ratio.
     */
    private fun fitToPaper(image: BufferedImage, paperWidth: PaperWidth): BufferedImage {
        val targetWidth = paperWidth.dots
        val scale = targetWidth.toDouble() / image.width

        // Don't upscale smaller images unnecessarily
        if (scale >= 1.0 && image.width <= targetWidth) return image

        val targetHeight = (image.height * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY)
        val g = scaled.createGraphics()
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null)
        g.dispose()
        return scaled
    }

    /**
     * Floyd-Steinberg dithering for high-quality 1-bit output on thermal printers.
     */
    private fun ditherFloydSteinberg(image: BufferedImage): BufferedImage {
        val w = image.width
        val h = image.height
        val bw = BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY)

        // Read grayscale values
        val gray = Array(h) { IntArray(w) }
        for (y in 0 until h) {
            for (x in 0 until w) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            }
        }

        // Floyd-Steinberg error diffusion
        for (y in 0 until h) {
            for (x in 0 until w) {
                val oldPixel = gray[y][x]
                val newPixel = if (oldPixel < 128) 0 else 255
                bw.setRGB(x, y, if (newPixel == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())

                val error = oldPixel - newPixel

                if (x + 1 < w) gray[y][x + 1] = (gray[y][x + 1] + (error * 7 / 16)).coerceIn(0, 255)
                if (y + 1 < h) {
                    if (x > 0) gray[y + 1][x - 1] = (gray[y + 1][x - 1] + (error * 3 / 16)).coerceIn(0, 255)
                    gray[y + 1][x] = (gray[y + 1][x] + (error * 5 / 16)).coerceIn(0, 255)
                    if (x + 1 < w) gray[y + 1][x + 1] = (gray[y + 1][x + 1] + (error * 1 / 16)).coerceIn(0, 255)
                }
            }
        }

        return bw
    }

    /**
     * Build ESC/POS raster command from a monochrome bitmap.
     *
     * Format: GS 'v' '0' m xL xH yL yH [raster data]
     * where m = 0 (normal), 1 (double-width), etc.
     */
    private fun buildRasterCommand(bwImage: BufferedImage): ByteArray {
        val w = bwImage.width
        val h = bwImage.height
        val bytesPerRow = (w + 7) / 8
        val out = ByteArrayOutputStream()

        // Start raster graphics: GS v 0 m xL xH yL yH
        out.write(0x1D)
        out.write(0x76)
        out.write(0x30)
        out.write(0x00) // m = 0 (normal)

        // Width in bytes (xL, xH)
        out.write((bytesPerRow % 256).toByte())
        out.write((bytesPerRow / 256).toByte())

        // Height in dots (yL, yH)
        out.write((h % 256).toByte())
        out.write((h / 256).toByte())

        // Raster data: for each row, convert pixels to 1-bit bytes
        val rowBuffer = ByteArray(bytesPerRow)
        for (y in 0 until h) {
            rowBuffer.fill(0)
            for (byteIdx in 0 until bytesPerRow) {
                var byteVal = 0
                for (bitIdx in 0 until 8) {
                    val px = byteIdx * 8 + bitIdx
                    if (px < w) {
                        val rgb = bwImage.getRGB(px, y)
                        val isBlack = (rgb and 0xFF) < 128
                        if (isBlack) {
                            byteVal = byteVal or (0x80 shr bitIdx)
                        }
                    }
                }
                rowBuffer[byteIdx] = byteVal.toByte()
            }
            out.write(rowBuffer)
        }

        return out.toByteArray()
    }
}
