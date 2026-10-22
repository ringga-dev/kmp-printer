package ngga.ring.printer.util.escpos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidESCPosRenderer : ESCPosRenderer {
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = withContext(Dispatchers.IO) {
        try {
            // PdfRenderer requires a file or seekable descriptor
            val tempFile = File.createTempFile("print_job", ".pdf")
            FileOutputStream(tempFile).use { it.write(data) }
            
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                tempFile.delete()
                return@withContext null
            }
            
            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt()
            
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            
            // Convert to bitonal using ESCPosImageHelper logic (simulated here)
            val pixels = IntArray(targetWidth * targetHeight)
            bitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            
            // Extract grayscale
            val grayscale = IntArray(pixels.size) { i ->
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            val result = ESCPosImageHelper.applyFloydSteinberg(grayscale, targetWidth, targetHeight)
            
            page.close()
            renderer.close()
            pfd.close()
            tempFile.delete()
            
            result
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? {
        // SVG rendering on Android usually requires a library like AndroidSVG.
        // For now, we stub it as it's less common than PDF in POS.
        return null
    }

    override fun pdfPageCount(data: ByteArray): Int {
        var tempFile: File? = null
        return try {
            val f = File.createTempFile("page_count", ".pdf")
            tempFile = f
            FileOutputStream(f).use { it.write(data) }
            val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            0
        } finally {
            tempFile?.delete()
        }
    }
}

actual fun getPlatformRenderer(): ESCPosRenderer = AndroidESCPosRenderer()
