package ngga.ring.printer.util.platform

/**
 * Common image processing interface for converting various platform image formats 
 * (Bitmap, UIImage, BufferedImage) to bitonal raster data.
 */
expect object ESCPosImageHelper {
    
    /**
     * Resizes and converts a platform-specific image to 1-bit raster data.
     * 
     * @param image The source image (Type: android.graphics.Bitmap, UIImage, or BufferedImage)
     * @param maxWidth Max width in dots (usually 384 or 576)
     * @return Triple of (bytes, width, height)
     */
    fun processToRaster(image: Any, maxWidth: Int = 384): Triple<ByteArray, Int, Int>

    /**
     * Decodes raw image file bytes (PNG, JPEG, BMP, GIF, WebP) into 1-bit raster data.
     *
     * This is the entry point for printing an image straight from a file or a
     * network response, without the caller having to build a platform-specific
     * image object first.
     *
     * @param data The encoded image file bytes.
     * @param maxWidth Max width in dots (usually 384 for 58mm or 576 for 80mm).
     * @return Triple of (bytes, width, height), or `null` when the bytes are not a
     *         decodable image. Returning `null` rather than throwing keeps a bad
     *         download from crashing the print job.
     */
    suspend fun decodeImageBytes(data: ByteArray, maxWidth: Int = 384): Triple<ByteArray, Int, Int>?
}
