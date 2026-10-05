package ngga.ring.printer.util.platform

import kotlinx.browser.document
import ngga.ring.printer.util.escpos.ESCPosImageHelper as CommonHelper
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * JS implementation of image processing, backed by an offscreen `<canvas>`.
 *
 * Dithering and raster packing are the shared commonMain implementations, so a
 * given image produces the same bytes here as on Android, JVM and iOS.
 *
 * Accepted inputs:
 *  - [HTMLImageElement], the usual case in the browser
 *  - [HTMLCanvasElement], when the caller already rasterised the image
 *  - an `ImageBitmap`, which Compose web hands to callers
 */
actual object ESCPosImageHelper {

    actual fun processToRaster(image: Any, maxWidth: Int): Triple<ByteArray, Int, Int> {
        val width = maxWidth.coerceAtLeast(1)

        // Resolved dynamically because ImageBitmap is not in the Kotlin DOM API.
        val source: dynamic = image
        val sourceWidth: Int = when (image) {
            is HTMLImageElement -> image.width
            is HTMLCanvasElement -> image.width
            else -> (source.width as Number).toInt()
        }
        val sourceHeight: Int = when (image) {
            is HTMLImageElement -> image.height
            is HTMLCanvasElement -> image.height
            else -> (source.height as Number).toInt()
        }

        if (!accepts(image)) {
            throw IllegalArgumentException(
                "JS image processing expects an HTMLImageElement, ImageBitmap or " +
                    "HTMLCanvasElement",
            )
        }
        require(sourceWidth > 0 && sourceHeight > 0) { "Image has no intrinsic size" }

        val targetHeight = ((sourceHeight.toDouble() * width) / sourceWidth)
            .toInt()
            .coerceAtLeast(1)

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = width
        canvas.height = targetHeight

        val ctx = canvas.getContext("2d")

        // A transparent background reads as black after thresholding, so the
        // canvas is filled first.
        ctx.asDynamic().fillStyle = "white"
        ctx.asDynamic().fillRect(0, 0, width, targetHeight)
        ctx.asDynamic().drawImage(image.asDynamic(), 0, 0, width, targetHeight)

        // getImageData is called dynamically too: the Kotlin DOM binding types
        // its arguments as Double and its result as a typed array, both of
        // which fight the Kotlin types used here.
        val data: dynamic = ctx.asDynamic().getImageData(0, 0, width, targetHeight).data
        val gray = IntArray(width * targetHeight)
        for (index in 0 until gray.size) {
            val r = (data[index * 4] as Number).toInt()
            val g = (data[index * 4 + 1] as Number).toInt()
            val b = (data[index * 4 + 2] as Number).toInt()
            gray[index] = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        }

        val bitonal = CommonHelper.applyFloydSteinberg(gray, width, targetHeight)
        val bytes = CommonHelper.packPixelsToRaster(bitonal, width, targetHeight)
        return Triple(bytes, width, targetHeight)
    }

    /**
     * Suspending variant for an `<img>` that has not finished decoding.
     * Browsers report `width` as 0 until the image loads.
     */
    suspend fun processToRasterAsync(
        image: HTMLImageElement,
        maxWidth: Int = 384,
    ): Triple<ByteArray, Int, Int> {
        if (!image.complete || image.naturalWidth == 0) {
            awaitLoad(image)
        }
        return processToRaster(image, maxWidth)
    }

    private suspend fun awaitLoad(image: HTMLImageElement) = suspendCoroutine { continuation ->
        val onLoad: (Event) -> Unit = { continuation.resume(Unit) }
        val onError: (Event) -> Unit = {
            continuation.resumeWithException(IllegalStateException("Image failed to decode"))
        }
        image.addEventListener("load", onLoad)
        image.addEventListener("error", onError)
    }

    /** True for the DOM image types this helper can draw. */
    private fun accepts(image: Any): Boolean =
        image is HTMLImageElement ||
            image is HTMLCanvasElement ||
            hasDrawableSize(image)

    private fun hasDrawableSize(image: Any): Boolean {
        val dynamicImage = image.asDynamic()
        return dynamicImage != null &&
            dynamicImage.width != null &&
            dynamicImage.height != null
    }
}