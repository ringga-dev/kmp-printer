package ngga.ring.printer.util.platform

import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.Color
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper

/**
 * JVM/Desktop implementation of image processing using AWT.
 */
actual object ESCPosImageHelper {
    
    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val original = if (image is ByteArray) {
            javax.imageio.ImageIO.read(image.inputStream())
        } else {
            image as BufferedImage
        }
        
        // 1. Scaling
        val scale = maxWidth.toDouble() / original.width.toDouble()
        val targetWidth = maxWidth
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.drawImage(original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null)
        g.dispose()
        
        // 2. Pixel extraction to grayscale
        val width = scaled.width
        val height = scaled.height
        val gray = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = Color(scaled.getRGB(x, y), true)
                // Standard luminance formula
                gray[y * width + x] = (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114).toInt()
            }
        }
        
        // 3. Dithering and packing live in commonMain so every platform
        // produces identical output from identical pixels.
        val bitonal = CommonHelper.applyFloydSteinberg(gray, width, height)
        val bytes = CommonHelper.packPixelsToRaster(bitonal, width, height)
        return Triple(bytes, width, height)
    }
}