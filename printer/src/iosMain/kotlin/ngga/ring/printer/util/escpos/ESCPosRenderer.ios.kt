package ngga.ring.printer.util.escpos

import platform.Foundation.*
import platform.PDFKit.*
import platform.UIKit.*
import platform.CoreGraphics.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * iOS Implementation using PDFKit and CoreGraphics.
 */
class IosESCPosRenderer : ESCPosRenderer {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = withContext(Dispatchers.Default) {
        try {
            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }
            
            val document = PDFDocument(data = nsData) ?: return@withContext null
            if (pageIndex >= document.pageCount.toInt()) return@withContext null
            
            val page = document.pageAtIndex(pageIndex.toULong()) ?: return@withContext null
            
            // Get the page bounds
            val pageSize = page.boundsForBox(kPDFDisplayBoxMediaBox)
            val scale = targetWidth.toDouble() / pageSize.useContents { size.width }
            val targetHeight = (pageSize.useContents { size.height } * scale).toInt()
            
            // Render to a bitonal-ready image
            UIGraphicsBeginImageContextWithOptions(
                CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble()),
                false, 
                1.0
            )
            
            val context = UIGraphicsGetCurrentContext() ?: return@withContext null
            
            // Flip the context (PDF and CoreGraphics have different coordinate systems)
            CGContextTranslateCTM(context, 0.0, targetHeight.toDouble())
            CGContextScaleCTM(context, 1.0, -1.0)
            
            // Draw the PDF page
            page.drawWithBox(kPDFDisplayBoxMediaBox, toContext = context)
            
            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            
            if (image == null) return@withContext null
            
            // Process pixels to BooleanArray
            processUIImageToBitonal(image, targetWidth, targetHeight)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun processUIImageToBitonal(image: UIImage, width: Int, height: Int): BooleanArray {
        val result = BooleanArray(width * height)
        val colorSpace = CGColorSpaceCreateDeviceGray()
        val bytesPerPixel = 1uL
        val bytesPerRow = width.toULong() * bytesPerPixel
        val rawData = nativeHeap.allocArray<ByteVar>((width * height).toLong())
        
        val context = CGBitmapContextCreate(
            rawData,
            width.toULong(),
            height.toULong(),
            8uL,
            bytesPerRow,
            colorSpace,
            CGImageAlphaInfo.kCGImageAlphaNone.value
        )
        
        if (context != null) {
            UIGraphicsPushContext(context)
            image.drawInRect(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
            UIGraphicsPopContext()
            CGContextRelease(context)
            
            for (i in 0 until (width * height)) {
                val grayValue = rawData[i].toInt() and 0xFF
                result[i] = grayValue < 128
            }
        }
        
        nativeHeap.free(rawData)
        return result
    }

    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun pdfPageCount(data: ByteArray): Int {
        return try {
            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }
            val document = PDFDocument(data = nsData) ?: return 0
            document.pageCount.toInt()
        } catch (_: Exception) {
            0
        }
    }
}

actual fun getPlatformRenderer(): ESCPosRenderer = IosESCPosRenderer()
