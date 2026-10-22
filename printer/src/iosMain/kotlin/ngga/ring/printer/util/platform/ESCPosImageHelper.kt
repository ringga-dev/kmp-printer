package ngga.ring.printer.util.platform

import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import kotlinx.cinterop.*
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper

/**
 * iOS Implementation of Image Processing using CoreGraphics.
 * Provides high-performance grayscale conversion and Floyd-Steinberg dithering.
 */
actual object ESCPosImageHelper {

    @OptIn(ExperimentalForeignApi::class)
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val uiImage = image as UIImage
        
        // 1. Calculate scaling
        val originalWidth = uiImage.size.useContents { width }
        val originalHeight = uiImage.size.useContents { height }
        val scale = maxWidth.toDouble() / originalWidth
        val targetWidth = maxWidth
        val targetHeight = (originalHeight * scale).toInt()

        // 2. Render to Grayscale Context
        val colorSpace = CGColorSpaceCreateDeviceGray()
        val bytesPerPixel = 1uL
        val bytesPerRow = targetWidth.toULong() * bytesPerPixel
        val rawData = nativeHeap.allocArray<ByteVar>((targetWidth * targetHeight).toLong())
        
        val context = CGBitmapContextCreate(
            rawData,
            targetWidth.toULong(),
            targetHeight.toULong(),
            8uL,
            bytesPerRow,
            colorSpace,
            CGImageAlphaInfo.kCGImageAlphaNone.value
        ) ?: throw Exception("Failed to create CGContext for grayscale")

        UIGraphicsPushContext(context)
        uiImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble()))
        UIGraphicsPopContext()
        CGContextRelease(context)

        // 3. Process to Bitonal using Floyd-Steinberg Dithering
        val gray = DoubleArray(targetWidth * targetHeight)
        for (i in 0 until (targetWidth * targetHeight)) {
            gray[i] = (rawData[i].toInt() and 0xFF).toDouble()
        }
        nativeHeap.free(rawData)

        val bitonal = BooleanArray(targetWidth * targetHeight)
        
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val index = y * targetWidth + x
                val oldPixel = gray[index]
                val newPixel = if (oldPixel < 128) 0.0 else 255.0
                bitonal[index] = newPixel == 0.0
                
                val error = oldPixel - newPixel
                
                if (x + 1 < targetWidth) gray[index + 1] += error * 7.0 / 16.0
                if (y + 1 < targetHeight) {
                    if (x - 1 >= 0) gray[(y + 1) * targetWidth + x - 1] += error * 3.0 / 16.0
                    gray[(y + 1) * targetWidth + x] += error * 5.0 / 16.0
                    if (x + 1 < targetWidth) gray[(y + 1) * targetWidth + x + 1] += error * 1.0 / 16.0
                }
            }
        }

        // 4. Pack to ESC/POS Raster format
        val bytes = CommonHelper.packPixelsToRaster(bitonal, targetWidth, targetHeight)
        return Triple(bytes, targetWidth, targetHeight)
    }

    /**
     * Decodes PNG/JPEG/BMP/GIF/WebP bytes via ImageIO (CGImageSource) into a
     * UIImage, then reuses [processToRaster] so the dithering path stays shared.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun decodeImageBytes(data: ByteArray, maxWidth: Int): Triple<ByteArray, Int, Int>? {
        return try {
            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }
            val source = CGImageSourceCreateWithData(nsData, null) ?: return null
            val cgImage = CGImageSourceCreateImageAtIndex(source, 0uL, null) ?: return null
            val uiImage = UIImage.imageWithCGImage(cgImage)
            processToRaster(uiImage, maxWidth)
        } catch (_: Exception) {
            null
        }
    }
}
