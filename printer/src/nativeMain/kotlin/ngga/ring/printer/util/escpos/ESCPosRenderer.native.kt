package ngga.ring.printer.util.escpos

/**
 * Native Renderer Stub.
 */
class NativeESCPosRenderer : ESCPosRenderer {
    override suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray? = null
    override suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray? = null
}

actual fun getPlatformRenderer(): ESCPosRenderer = NativeESCPosRenderer()
