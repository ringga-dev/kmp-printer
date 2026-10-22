package ngga.ring.printer.util.escpos

/**
 * Web renderer.
 *
 * **PDF rendering is not available on the web target.** Browsers have no
 * built-in PDF rasterizer, and shipping one (pdf.js, ~1 MB plus a web worker) is
 * a packaging decision that belongs to the application, not to this library.
 * Both [renderPdfPage] and [pdfPageCount] therefore report "unsupported" rather
 * than throwing, so callers can show a clear message instead of hitting a silent
 * no-op.
 *
 * Image files *are* supported on the web through
 * [ngga.ring.printer.util.platform.ESCPosImageHelper.decodeImageBytes], which
 * hands the bytes to the browser's own decoder.
 */
class WebESCPosRenderer : ESCPosRenderer {
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = null
    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? = null

    /** Always `0`: the web target has no PDF reader. */
    override fun pdfPageCount(data: ByteArray): Int = 0
}

actual fun getPlatformRenderer(): ESCPosRenderer = WebESCPosRenderer()
