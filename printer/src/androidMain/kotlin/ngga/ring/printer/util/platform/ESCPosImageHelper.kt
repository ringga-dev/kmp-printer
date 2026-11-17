package ngga.ring.printer.util.platform

import android.graphics.Bitmap
import android.graphics.Color
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper

/**
 * Android implementation of image processing.
 */
actual object ESCPosImageHelper {
    
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val original = if (image is ByteArray) {
            android.graphics.BitmapFactory.decodeByteArray(image, 0, image.size)
        } else {
            image as android.graphics.Bitmap
        }
        
        // 1. Scaling to fit paper width
        val scale = maxWidth.toDouble() / original.width.toDouble()
        val targetWidth = maxWidth
        val targetHeight = (original.height * scale).toInt()
        
        val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        
        // 2. Pixel extraction
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 3. Grayscale conversion (Floyd-Steinberg)
        val gray = DoubleArray(width * height) { i ->
            val color = pixels[i]
            (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114)
        }
        
        val bitonal = BooleanArray(width * height)
        
        // Floyd-Steinberg Dithering
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val oldPixel = gray[index]
                val newPixel = if (oldPixel < 128) 0.0 else 255.0
                bitonal[index] = newPixel == 0.0 // 0.0 (Black) is true
                
                val error = oldPixel - newPixel
                
                if (x + 1 < width) gray[index + 1] += error * 7.0 / 16.0
                if (y + 1 < height) {
                    if (x - 1 >= 0) gray[(y + 1) * width + x - 1] += error * 3.0 / 16.0
                    gray[(y + 1) * width + x] += error * 5.0 / 16.0
                    if (x + 1 < width) gray[(y + 1) * width + x + 1] += error * 1.0 / 16.0
                }
            }
        }
        
        // 4. Packing
        val bytes = CommonHelper.packPixelsToRaster(bitonal, width, height)
        
        if (scaled != original) scaled.recycle()
        
        return Triple(bytes, width, height)
    }

    actual suspend fun decodeImageBytes(data: ByteArray, maxWidth: Int): Triple<ByteArray, Int, Int>? {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: return null
            processToRaster(bitmap, maxWidth)
        } catch (_: Exception) {
            null
        }
    }
}
