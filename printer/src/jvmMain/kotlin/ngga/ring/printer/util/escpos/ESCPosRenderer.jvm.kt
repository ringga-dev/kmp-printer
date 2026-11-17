package ngga.ring.printer.util.escpos

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.rendering.ImageType
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.util.escpos.ESCPosRenderer

/**
 * Desktop/JVM Implementation using Apache PDFBox.
 */
class JvmESCPosRenderer : ESCPosRenderer {
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = withContext(Dispatchers.IO) {
        try {
            val document = Loader.loadPDF(data)
            val renderer = PDFRenderer(document)
            
            // Render at a DPI that matches our target width
            // Heuristic: 72 DPI is standard. targetWidth / 72 * ...
            val bim = renderer.renderImage(pageIndex, 2.0f, ImageType.GRAY) // 2x scale for better detail
            document.close()

            // Resize to target width if needed
            val scaled = if (bim.width != targetWidth) {
                val height = (targetWidth.toDouble() / bim.width * bim.height).toInt()
                val res = BufferedImage(targetWidth, height, BufferedImage.TYPE_BYTE_GRAY)
                val g = res.createGraphics()
                g.drawImage(bim, 0, 0, targetWidth, height, null)
                g.dispose()
                res
            } else {
                bim
            }

            // Convert to bitonal
            val result = BooleanArray(scaled.width * scaled.height)
            val raster = scaled.raster
            val pixelBuffer = IntArray(1)
            for (y in 0 until scaled.height) {
                for (x in 0 until scaled.width) {
                    raster.getPixel(x, y, pixelBuffer)
                    result[y * scaled.width + x] = pixelBuffer[0] < 128
                }
            }
            
            result
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? = null

    override fun pdfPageCount(data: ByteArray): Int {
        return try {
            Loader.loadPDF(data).use { document -> document.numberOfPages }
        } catch (_: Exception) {
            0
        }
    }
}

actual fun getPlatformRenderer(): ESCPosRenderer = JvmESCPosRenderer()
